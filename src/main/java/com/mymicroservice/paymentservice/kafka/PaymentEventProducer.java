package com.mymicroservice.paymentservice.kafka;

import org.mymicroservices.common.events.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;
    private final String topic = "create-payment";

    public void sendCreatePayment(PaymentEventDto event) {
        kafkaTemplate.send(topic, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send CREATE_PAYMENT for paymentId={}", event.getId(), ex);
                    } else {
                        log.info("CREATE_PAYMENT sent paymentId={}, offset={}", event.getId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

}
