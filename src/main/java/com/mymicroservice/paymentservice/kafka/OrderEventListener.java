package com.mymicroservice.paymentservice.kafka;

import com.mymicroservice.paymentservice.util.KafkaMdcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;
    private static final Logger TRACE_LOGGER = LoggerFactory.getLogger("TRACE_MDC_LOGGER");

    @KafkaListener(topics = "create-order", groupId = "payment-service-group")
    public void onCreateOrder(
            @Payload OrderEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Message<OrderEventDto> message) {

        KafkaMdcUtil.restoreMdcFromMessage(message);
        MDC.put("serviceName", "paymentservice");

        try {
            TRACE_LOGGER.info("Received CREATE_ORDER for orderId={}", event.getOrderId());
            log.info("Received CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, event);

            PaymentEventDto saved = paymentService.createPayment(event);

            log.info("Successfully processed payment for order: {}", event.getOrderId());
            TRACE_LOGGER.info("Successfully processed payment for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage(), e);
            TRACE_LOGGER.error("Error processing CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}
