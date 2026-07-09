package com.mymicroservice.paymentservice.util;

import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;

import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;

public class OrderEventDtoGenerator {

    public static OrderEventDto generateOrderEventDto() {
        return generateOrderEventDto(ENTITY_ID, ENTITY_ID, BigDecimal.valueOf(1000.00));
    }

    public static OrderEventDto generateOrderEventDto(String orderId) {
        return generateOrderEventDto(orderId, ENTITY_ID, BigDecimal.valueOf(1000.00));
    }

    public static OrderEventDto generateOrderEventDto(String orderId, String userId, BigDecimal amount) {
        return OrderEventDto.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentAmount(amount)
                .build();
    }
}
