package com.mymicroservice.paymentservice.integration.kafka;

import com.mymicroservice.paymentservice.configuration.AbstractKafkaIntegrationTest;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.util.EventEnvelopeGenerator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mymicroservice.paymentservice.util.CommonConstants.CREATE_PAYMENT_EVENT;
import static com.mymicroservice.paymentservice.util.CommonConstants.EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.CommonConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_PAYMENT_TOPIC;
import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@EmbeddedKafka(partitions = 1, topics = {CREATE_PAYMENT_TOPIC})
@DirtiesContext
class PaymentEventProducerIT extends AbstractKafkaIntegrationTest {

    @Autowired
    private PaymentEventProducer paymentEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private InboxScheduler inboxScheduler;

    @Test
    void sendCreatePayment_ShouldPublishEventToKafka_WhenPaymentCreated() {
        paymentEventProducer.sendCreatePayment(EventEnvelopeGenerator.generatePaymentEventEnvelope());

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "payment-producer-it-group", "true", embeddedKafkaBroker);
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

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500));
            assertThat(records.count()).isGreaterThan(0);

            ConsumerRecord<String, PaymentEventDto> record = records.iterator().next();
            assertThat(record.value().getOrderId()).isEqualTo(ENTITY_ID);
            assertThat(new String(record.headers().lastHeader(IDEMPOTENCE_ID).value(), StandardCharsets.UTF_8))
                    .isEqualTo(com.mymicroservice.paymentservice.util.data.TestConstants.IDEMPOTENCE_ID);
            assertThat(new String(record.headers().lastHeader(EVENT_TYPE).value(), StandardCharsets.UTF_8))
                    .isEqualTo(CREATE_PAYMENT_EVENT);
        });

        consumer.close();
    }
}
