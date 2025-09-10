package com.mymicroservice.paymentservice.kafka;

import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.mymicroservice.paymentservice.service.PaymentService;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "create-order", groupId = "payment-service-group")
    public void onCreateOrder(OrderEventDto event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Received CREATE_ORDER event: {}", event);

        PaymentEventDto saved = paymentService.createPayment(event);
    }
}
