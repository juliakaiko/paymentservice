package com.mymicroservice.paymentservice.kafka;

import com.mymicroservice.paymentservice.util.KafkaMdcUtil;
import org.mymicroservices.common.events.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;
    private final String TOPIC = "create-payment";

    public void sendCreatePayment(PaymentEventDto event) {

        log.info("Producing CREATE_PAYMENT for orderId={}", event.getOrderId());

        /**
         * Creating a message with MDC
         */
        Message<PaymentEventDto> message = KafkaMdcUtil.addMdcToMessage(
                event,
                event.getOrderId(),
                TOPIC
        );

        kafkaTemplate.send(message)
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
