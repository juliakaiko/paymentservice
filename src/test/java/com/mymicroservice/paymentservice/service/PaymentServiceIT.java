package com.mymicroservice.paymentservice.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.kafka.OrderEventListener;
import com.mymicroservice.paymentservice.mapper.PaymentRequestMapper;
import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.impl.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@DirtiesContext
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0) // WireMock will work on a random port
@Slf4j
public class PaymentServiceIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @Container
    static MongoDBContainer mongoDB = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @Autowired
    private OrderEventListener orderEventListener;

    @Autowired
    private PaymentServiceImpl paymentService;

    private PaymentRequestDto paymentRequestDto;
    private OrderEventDto orderEventDto;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("random.number.api.base-url", () -> "http://localhost:${wiremock.server.port}");
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
    }

    @BeforeAll
    static void setupContainers() {
        kafka.start();
        mongoDB.start();
    }

    @BeforeEach
    void init() {
        paymentRequestDto = PaymentRequestDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
        orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);

        WireMock.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("min", equalTo("1"))
                .withQueryParam("max", equalTo("100"))
                .withQueryParam("count", equalTo("1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[42]")
                        .withStatus(200)));
    }

    @AfterEach
    void tearDown() {
        WireMock.reset(); // Resetting WireMock state after each test
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_storesPaymentInMongo() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("createPayment_storesPaymentInMongo(): PaymentEventDto={}", dto);

        assertNotNull(dto.getId());
        assertEquals("1", dto.getOrderId());
        assertEquals("1", dto.getUserId());
        assertEquals(Status.PAID.name(), dto.getStatus());

        PaymentEntity saved = paymentRepository.findById(dto.getId()).orElseThrow();

        assertEquals(dto.getOrderId(), saved.getOrderId());

        verifyWebClientCall();
    }

    @Test
    void testCreatePayment_sendsEventToKafka() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("testCreatePayment_sendsEventToKafka(): PaymentEventDto={}", dto);

        assertThat(dto).isNotNull();
        assertThat(paymentRepository.findByOrderId("1")).hasSize(1);
        PaymentEntity savedEntity = paymentRepository.findByOrderId("1").get(0);
        assertThat(savedEntity.getStatus()).isNotNull();

        // check that the event has been sent to Kafka
        Map<String, Object> consumerProps = Map.of(
                "bootstrap.servers", kafka.getBootstrapServers(),
                "group.id", "payment-service-test-group",
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer", "org.springframework.kafka.support.serializer.JsonDeserializer",
                "auto.offset.reset", "earliest",
                "enable.auto.commit", "true",
                "spring.json.trusted.packages", "*"
        );
        ConsumerFactory<String, PaymentEventDto> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, PaymentEventDto> consumer = cf.createConsumer();
        consumer.subscribe(java.util.Collections.singleton("create-payment"));

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        ConsumerRecord<String, PaymentEventDto> firstRecord = records.iterator().next();
        assertThat(firstRecord.value().getOrderId()).isEqualTo("1");
        assertThat(firstRecord.value().getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));

        consumer.close();
    }

    @Test
    void getPaymentById_returnsPaymentEventDto() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("getPaymentById_returnsPaymentEventDto(): PaymentId={}", dto.getId());

        PaymentEventDto fetched = paymentService.getPaymentById(dto.getId());

        assertEquals(dto.getId(), fetched.getId());
        assertEquals(dto.getOrderId(), fetched.getOrderId());
    }

    @Test
    void updatePayment_returnsUpdatedPaymentEventDto() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("updatePayment_returnsUpdatedPaymentEventDto(): PaymentId={}", dto.getId());

        PaymentEventDto updated = paymentService.updatePayment(dto.getId(), orderEventDto);

        assertNotNull(updated);
        assertEquals(orderEventDto.getOrderId(), updated.getOrderId());
        assertEquals(orderEventDto.getUserId(), updated.getUserId());
        assertNotNull(updated.getStatus());
    }

    @Test
    void deletePayment_removesFromMongo() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("deletePayment_removesFromMongo(): PaymentId={}", dto.getId());

        PaymentEventDto deleted = paymentService.deletePaymentById(dto.getId());
        assertEquals(dto.getId(), deleted.getId());
        assertFalse(paymentRepository.findById(dto.getId()).isPresent());
    }

    @Test
    void getPaymentsByOrderId_returnsList() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("getPaymentsByOrderId_returnsList()");

        List<PaymentEventDto> list = paymentService.getPaymentsByOrderId("1");

        assertFalse(list.isEmpty());
        assertEquals(dto.getOrderId(), list.get(0).getOrderId());
    }

    @Test
    void getPaymentsByUserId_returnsList() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("getPaymentsByUserId_returnsList()");

        List<PaymentEventDto> list = paymentService.getPaymentsByUserId("1");

        assertFalse(list.isEmpty());
        assertEquals(dto.getUserId(), list.get(0).getUserId());
    }

    @Test
    void getPaymentsByStatuses_returnsList() {
        PaymentEventDto dto = paymentService.createPayment(orderEventDto);
        log.info("getPaymentsByStatuses_returnsList()");

        List<PaymentEventDto> list = paymentService.getPaymentsByStatuses(List.of(Status.PAID.name(), Status.FAILED.name()));

        assertFalse(list.isEmpty());
        assertTrue(List.of(Status.PAID.name(), Status.FAILED.name()).contains(list.get(0).getStatus()));
    }

    @Test
    void getTotalSumForPeriod_returnsSum() {
        PaymentEventDto dto1 = paymentService.createPayment(orderEventDto);
        PaymentRequestDto req2 = PaymentRequestDto.builder()
                .orderId("2")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
        OrderEventDto order2 = PaymentRequestMapper.INSTANCE.toOrderEventDto(req2);
        PaymentEventDto dto2 = paymentService.createPayment(order2);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        log.info("getTotalSumForPeriod_returnsSum(): {} - {}", start, end);

        BigDecimal sum = paymentService.getTotalSumForPeriod(start, end);

        assertEquals(BigDecimal.valueOf(2000.00), sum);
    }

    private void verifyWebClientCall() {
        WireMock.verify(1, getRequestedFor(urlPathEqualTo("/"))
                .withQueryParam("min", equalTo("1"))
                .withQueryParam("max", equalTo("100"))
                .withQueryParam("count", equalTo("1")));
    }
}
