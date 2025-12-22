package com.mymicroservice.paymentservice.kafka;

import com.mymicroservice.paymentservice.util.KafkaMdcUtil;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;
    private final String topic = "create-payment";
    private static final Logger TRACE_LOGGER = LoggerFactory.getLogger("TRACE_MDC_LOGGER");

    public void sendCreatePayment(PaymentEventDto event) {

        TRACE_LOGGER.info("Sending CREATE_PAYMENT for orderId={}", event.getOrderId());
        log.info("Producing CREATE_PAYMENT for orderId={}", event.getOrderId());

        Message<PaymentEventDto> message = KafkaMdcUtil.addMdcToMessage(
                event,
                event.getOrderId(),
                topic
        );

        //kafkaTemplate.send(topic, event.getOrderId(), event)
        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send CREATE_PAYMENT for paymentId={}", event.getId(), ex);
                        TRACE_LOGGER.info("Failed to send CREATE_PAYMENT for paymentId={}", event.getId(), ex);
                    } else {
                        log.info("CREATE_PAYMENT sent paymentId={}, offset={}", event.getId(),
                                result.getRecordMetadata().offset());
                        TRACE_LOGGER.info("CREATE_PAYMENT sent paymentId={}, offset={}", event.getId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

}
