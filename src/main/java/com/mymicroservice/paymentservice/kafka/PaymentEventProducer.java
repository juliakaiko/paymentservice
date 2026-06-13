package com.mymicroservice.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static com.mymicroservice.paymentservice.util.CommonConstants.CREATE_PAYMENT_EVENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @Value("${kafka.producer.topics.create-payment}")
    private String paymentTopic;

    public void sendCreatePayment(EventEnvelope<PaymentEventDto> eventEnvelope) {

        log.info("Producing {} for orderId={}", CREATE_PAYMENT_EVENT, eventEnvelope.payload().getOrderId());

        Message<PaymentEventDto> message = MessageBuilder
                .withPayload(eventEnvelope.payload())
                .setHeader(KafkaHeaders.TOPIC, paymentTopic)
                .setHeader(KafkaHeaders.KEY, eventEnvelope.payload().getOrderId())
                .setHeader("X-Trace-Id", eventEnvelope.traceId())
                .setHeader("X-Source-Service", eventEnvelope.sourceService())
                .setHeader("X-Idempotence-Id", eventEnvelope.idempotenceId())
                .setHeader("X-Event-Type", CREATE_PAYMENT_EVENT)
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send {} for paymentId={}",
                                CREATE_PAYMENT_EVENT,
                                eventEnvelope.payload().getId(),
                                ex.getMessage()
                        );
                    } else {
                        log.info("{} sent paymentId={}, offset={}", CREATE_PAYMENT_EVENT, eventEnvelope.payload().getId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
