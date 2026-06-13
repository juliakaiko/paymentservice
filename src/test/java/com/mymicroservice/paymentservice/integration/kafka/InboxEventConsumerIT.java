package com.mymicroservice.paymentservice.integration.kafka;

import com.mymicroservice.paymentservice.configuration.AbstractKafkaIntegrationTest;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.util.KafkaTestMessageSender;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_TOPIC;
import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TEST_IDEMPOTENCE_UUID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@EmbeddedKafka(partitions = 1, topics = {CREATE_ORDER_TOPIC})
@DirtiesContext
class InboxEventConsumerIT extends AbstractKafkaIntegrationTest {

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    @MockBean
    private InboxScheduler inboxScheduler;

    @BeforeEach
    void setUp() {
        inboxEventRepository.deleteAll();
    }

    @Test
    void onCreateOrder_ShouldPersistReceivedEvent_WhenValidMessageConsumed() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        KafkaTestMessageSender.sendOrderEvent(
                kafkaTemplate, event, TEST_IDEMPOTENCE_UUID,
                CREATE_ORDER_EVENT_TYPE, TRACE_ID, SOURCE_SERVICE);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(inboxEventRepository.count()).isEqualTo(1);
            assertThat(inboxEventRepository.findAll().get(0).getStatus()).isEqualTo(InboxEventStatus.RECEIVED);
            assertThat(inboxEventRepository.findAll().get(0).getIdempotenceId()).isEqualTo(TEST_IDEMPOTENCE_UUID);
        });
    }

    @Test
    void onCreateOrder_ShouldIgnoreDuplicate_WhenSameIdempotenceIdSentTwice() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        KafkaTestMessageSender.sendOrderEvent(
                kafkaTemplate, event, TEST_IDEMPOTENCE_UUID,
                CREATE_ORDER_EVENT_TYPE, TRACE_ID, SOURCE_SERVICE);
        KafkaTestMessageSender.sendOrderEvent(
                kafkaTemplate, event, TEST_IDEMPOTENCE_UUID,
                CREATE_ORDER_EVENT_TYPE, TRACE_ID, SOURCE_SERVICE);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(inboxEventRepository.count()).isEqualTo(1));
    }

    @Test
    void onCreateOrder_ShouldPersistPayloadWithOrderId_WhenMessageConsumed() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        UUID idempotenceId = UUID.randomUUID();
        KafkaTestMessageSender.sendOrderEvent(
                kafkaTemplate, event, idempotenceId,
                CREATE_ORDER_EVENT_TYPE, TRACE_ID, SOURCE_SERVICE);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(inboxEventRepository.count()).isEqualTo(1);
            String payload = inboxEventRepository.findAll().get(0).getPayload();
            assertThat(payload).contains(ENTITY_ID);
        });
    }
}
