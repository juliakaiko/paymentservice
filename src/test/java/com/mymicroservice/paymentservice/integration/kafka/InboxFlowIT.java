package com.mymicroservice.paymentservice.integration.kafka;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mymicroservice.paymentservice.configuration.AbstractKafkaIntegrationTest;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.service.InboxService;
import com.mymicroservice.paymentservice.util.InboxEventGenerator;
import com.mymicroservice.paymentservice.util.KafkaTestMessageSender;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.mymicroservice.paymentservice.util.CommonConstants.CREATE_PAYMENT_EVENT;
import static com.mymicroservice.paymentservice.util.CommonConstants.EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.CommonConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_TOPIC;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_PAYMENT_TOPIC;
import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TEST_IDEMPOTENCE_UUID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@EmbeddedKafka(partitions = 1, topics = {CREATE_ORDER_TOPIC, CREATE_PAYMENT_TOPIC})
@AutoConfigureWireMock(port = 0)
@DirtiesContext
class InboxFlowIT extends AbstractKafkaIntegrationTest {

    @Autowired
    private InboxService inboxService;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private InboxScheduler inboxScheduler;

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("random.number.api.base-url", () -> "http://localhost:${wiremock.server.port}");
    }

    @BeforeEach
    void setUp() {
        inboxEventRepository.deleteAll();
        paymentRepository.deleteAll();

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
    }

    @Test
    void processPendingInboxEvents_ShouldCreatePaymentAndPublishEvent_WhenInboxHasReceivedEvent() {
        inboxService.saveInboxEvent(InboxEventGenerator.generateReceivedInboxEvent());

        inboxService.processPendingInboxEvents();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(inboxEventRepository.findAll().get(0).getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
            assertThat(paymentRepository.findByOrderId(ENTITY_ID)).hasSize(1);
            assertThat(paymentRepository.findByOrderId(ENTITY_ID).get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        });

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "inbox-flow-it-group", "true", embeddedKafkaBroker);
        consumerProps.put("spring.json.trusted.packages", "*");
        consumerProps.put("spring.json.value.default.type", PaymentEventDto.class.getName());
        consumerProps.put("value.deserializer",
                "org.springframework.kafka.support.serializer.JsonDeserializer");
        consumerProps.put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");

        ConsumerFactory<String, PaymentEventDto> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, PaymentEventDto> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList(CREATE_PAYMENT_TOPIC));

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
        ConsumerRecord<String, PaymentEventDto> record = null;
        for (var r : records) {
            if (ENTITY_ID.equals(r.value().getOrderId())) {
                record = r;
                break;
            }
        }
        assertThat(record).isNotNull();
        assertThat(record.value().getOrderId()).isEqualTo(ENTITY_ID);
        assertThat(record.value().getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
        assertThat(new String(record.headers().lastHeader(EVENT_TYPE).value(), StandardCharsets.UTF_8))
                .isEqualTo(CREATE_PAYMENT_EVENT);

        consumer.close();
    }

    @Test
    void processPendingInboxEvents_ShouldMarkFailedAndIncrementRetry_WhenPayloadIsInvalid() {
        inboxService.saveInboxEvent(
                InboxEventGenerator.generateInboxEvent(
                        InboxEventStatus.RECEIVED, 0, InboxEventGenerator.UNDESERIALIZABLE_PAYLOAD));

        inboxService.processPendingInboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var inboxEvent = inboxEventRepository.findAll().get(0);
            assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.FAILED);
            assertThat(inboxEvent.getRetryCount()).isEqualTo(1);
            assertThat(paymentRepository.count()).isZero();
        });
    }

    @Test
    void processPendingInboxEvents_ShouldMarkDead_WhenMaxRetriesExceeded() {
        inboxEventRepository.save(
                InboxEventGenerator.generateInboxEvent(
                        InboxEventStatus.FAILED, 9, InboxEventGenerator.UNDESERIALIZABLE_PAYLOAD));

        inboxService.processPendingInboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var inboxEvent = inboxEventRepository.findAll().get(0);
            assertThat(inboxEvent.getStatus()).isEqualTo(InboxEventStatus.DEAD);
            assertThat(inboxEvent.getRetryCount()).isEqualTo(10);
            assertThat(paymentRepository.count()).isZero();
        });
    }

    @Test
    void onCreateOrderAndProcess_ShouldCompleteFullFlow_WhenMessageSentToKafka() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        UUID idempotenceId = TEST_IDEMPOTENCE_UUID;
        KafkaTestMessageSender.sendOrderEvent(
                kafkaTemplate, event, idempotenceId,
                CREATE_ORDER_EVENT_TYPE, TRACE_ID, SOURCE_SERVICE);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(inboxEventRepository.count()).isEqualTo(1));

        inboxService.processPendingInboxEvents();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(inboxEventRepository.findAll().get(0).getStatus()).isEqualTo(InboxEventStatus.PROCESSED);
            assertThat(paymentRepository.findByOrderId(ENTITY_ID)).isNotEmpty();
        });
    }
}
