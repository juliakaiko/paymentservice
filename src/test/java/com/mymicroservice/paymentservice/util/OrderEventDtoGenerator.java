package com.mymicroservice.paymentservice.util;

import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;

public class OrderEventDtoGenerator {

    public static OrderEventDto generateOrderEventDto() {
        return generateOrderEventDto("1", "1", BigDecimal.valueOf(1000.00));
    }

    public static OrderEventDto generateOrderEventDto(String orderId) {
        return generateOrderEventDto(orderId, "1", BigDecimal.valueOf(1000.00));
    }

    public static OrderEventDto generateOrderEventDto(String orderId, String userId, BigDecimal amount) {
        return OrderEventDto.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentAmount(amount)
                .build();
    }
}
