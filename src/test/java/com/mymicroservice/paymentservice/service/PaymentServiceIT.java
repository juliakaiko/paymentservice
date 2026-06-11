package com.mymicroservice.paymentservice.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.impl.PaymentServiceImpl;
import com.mymicroservice.paymentservice.util.EventEnvelopeGenerator;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@Testcontainers
@DirtiesContext
@ActiveProfiles({"test", "dev"})
@AutoConfigureWireMock(port = 0)
class PaymentServiceIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

    @Container
    static MongoDBContainer mongoDB = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentServiceImpl paymentService;

    private EventEnvelope<OrderEventDto> eventEnvelope;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("random.number.api.base-url", () -> "http://localhost:${wiremock.server.port}");
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
    }

    @BeforeEach
    void init() {
        eventEnvelope = EventEnvelopeGenerator.generateOrderEventEnvelope();

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
        WireMock.reset();
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_ShouldStorePaymentInMongo_WhenOrderDoesNotExist() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        assertNotNull(dto.getId());
        assertEquals("1", dto.getOrderId());
        assertEquals("1", dto.getUserId());
        assertEquals(PaymentStatus.PAID.name(), dto.getStatus());

        Payment saved = paymentRepository.findById(dto.getId()).orElseThrow();
        assertEquals(dto.getOrderId(), saved.getOrderId());

        WireMock.verify(1, getRequestedFor(urlPathEqualTo("/"))
                .withQueryParam("min", equalTo("1"))
                .withQueryParam("max", equalTo("100"))
                .withQueryParam("count", equalTo("1")));
    }

    @Test
    void createPayment_ShouldReturnExistingPayment_WhenOrderAlreadyHasPayment() {
        PaymentEventDto first = paymentService.createPayment(eventEnvelope);
        PaymentEventDto second = paymentService.createPayment(eventEnvelope);

        assertEquals(first.getId(), second.getId());
        assertThat(paymentRepository.findByOrderId("1")).hasSize(1);
    }

    @Test
    void createPayment_ShouldSendEventToKafka_WhenPaymentCreated() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        assertThat(dto).isNotNull();
        assertThat(paymentRepository.findByOrderId("1")).hasSize(1);

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
        consumer.subscribe(Collections.singleton("create-payment"));

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        ConsumerRecord<String, PaymentEventDto> firstRecord = records.iterator().next();
        assertThat(firstRecord.value().getOrderId()).isEqualTo("1");
        assertThat(firstRecord.value().getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));

        consumer.close();
    }

    @Test
    void getPaymentById_ShouldReturnPaymentEventDto_WhenPaymentExists() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        PaymentEventDto fetched = paymentService.getPaymentById(dto.getId());

        assertEquals(dto.getId(), fetched.getId());
        assertEquals(dto.getOrderId(), fetched.getOrderId());
    }

    @Test
    void updatePayment_ShouldReturnUpdatedPaymentEventDto_WhenPaymentExists() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        PaymentEventDto updated = paymentService.updatePayment(dto.getId(), eventEnvelope.payload());

        assertNotNull(updated);
        assertEquals(eventEnvelope.payload().getOrderId(), updated.getOrderId());
        assertEquals(eventEnvelope.payload().getUserId(), updated.getUserId());
        assertNotNull(updated.getStatus());
    }

    @Test
    void deletePaymentById_ShouldRemovePayment_WhenPaymentExists() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        PaymentEventDto deleted = paymentService.deletePaymentById(dto.getId());

        assertEquals(dto.getId(), deleted.getId());
        assertFalse(paymentRepository.findById(dto.getId()).isPresent());
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnList_WhenPaymentsExist() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        List<PaymentEventDto> list = paymentService.getPaymentsByOrderId("1");

        assertFalse(list.isEmpty());
        assertEquals(dto.getOrderId(), list.get(0).getOrderId());
    }

    @Test
    void getPaymentsByUserId_ShouldReturnList_WhenPaymentsExist() {
        PaymentEventDto dto = paymentService.createPayment(eventEnvelope);

        List<PaymentEventDto> list = paymentService.getPaymentsByUserId("1");

        assertFalse(list.isEmpty());
        assertEquals(dto.getUserId(), list.get(0).getUserId());
    }

    @Test
    void getPaymentsByStatuses_ShouldReturnList_WhenPaymentsExist() {
        paymentService.createPayment(eventEnvelope);

        List<PaymentEventDto> list = paymentService.getPaymentsByStatuses(
                List.of(PaymentStatus.PAID.name(), PaymentStatus.FAILED.name()));

        assertFalse(list.isEmpty());
    }

    @Test
    void getTotalSumForPeriod_ShouldReturnSum_WhenPaymentsExistInPeriod() {
        paymentService.createPayment(eventEnvelope);

        EventEnvelope<OrderEventDto> secondEnvelope =
                EventEnvelopeGenerator.generateOrderEventEnvelope(OrderEventDtoGenerator.generateOrderEventDto("2"));
        paymentService.createPayment(secondEnvelope);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        BigDecimal sum = paymentService.getTotalSumForPeriod(start, end);

        assertEquals(BigDecimal.valueOf(2000.00), sum);
    }
}
