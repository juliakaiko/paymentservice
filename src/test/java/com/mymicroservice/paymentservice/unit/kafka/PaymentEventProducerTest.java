package com.mymicroservice.paymentservice.unit.kafka;

import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.util.EventEnvelopeGenerator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static com.mymicroservice.paymentservice.util.CommonConstants.CREATE_PAYMENT_EVENT;
import static com.mymicroservice.paymentservice.util.CommonConstants.EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_PAYMENT_TOPIC;
import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentEventProducer, "paymentTopic", CREATE_PAYMENT_TOPIC);
    }

    @Test
    void sendCreatePayment_ShouldSendMessageWithHeaders_WhenCalled() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(CREATE_PAYMENT_TOPIC, 0), 0, 0, 0, 0, 0);
        ProducerRecord<String, PaymentEventDto> producerRecord =
                new ProducerRecord<>(CREATE_PAYMENT_TOPIC, ENTITY_ID,
                        EventEnvelopeGenerator.generatePaymentEventEnvelope().payload());
        SendResult<String, PaymentEventDto> sendResult = new SendResult<>(producerRecord, metadata);
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        paymentEventProducer.sendCreatePayment(EventEnvelopeGenerator.generatePaymentEventEnvelope());

        ArgumentCaptor<Message<PaymentEventDto>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());

        Message<PaymentEventDto> message = captor.getValue();
        assertThat(message.getPayload().getOrderId()).isEqualTo(ENTITY_ID);
        assertThat(message.getHeaders().get(com.mymicroservice.paymentservice.util.CommonConstants.IDEMPOTENCE_ID))
                .isEqualTo(com.mymicroservice.paymentservice.util.data.TestConstants.IDEMPOTENCE_ID);
        assertThat(message.getHeaders().get(EVENT_TYPE)).isEqualTo(CREATE_PAYMENT_EVENT);
    }

    @Test
    void sendCreatePayment_ShouldInvokeKafkaTemplate_WhenCalled() {
        CompletableFuture<SendResult<String, PaymentEventDto>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka unavailable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        paymentEventProducer.sendCreatePayment(EventEnvelopeGenerator.generatePaymentEventEnvelope());

        verify(kafkaTemplate).send(any(Message.class));
    }
}
