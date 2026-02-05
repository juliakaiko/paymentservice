package com.mymicroservice.paymentservice.kafka;

import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.mymicroservice.paymentservice.service.PaymentService;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;
    private final String SERVICE_NAME = "paymentservice";

    @KafkaListener(topics = "create-order", groupId = "payment-service-group")
    public void onCreateOrder(
            @Payload OrderEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header("X-Request-Id") String requestId,
            @Header("X-Source-Service") String sourceService,
            Acknowledgment ack) {

        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        if (sourceService != null) {
            MDC.put("sourceService", sourceService);
        }
        MDC.put("serviceName", SERVICE_NAME);

        try {
            log.info("Received CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, event);

            PaymentEventDto saved = paymentService.createPayment(event);
            ack.acknowledge(); // commit offset
            log.info("Successfully processed payment for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage(), e);
            ack.nack(Duration.ofMillis(100)); // sleep and try again
        } finally {
            MDC.clear();
        }
    }
}
