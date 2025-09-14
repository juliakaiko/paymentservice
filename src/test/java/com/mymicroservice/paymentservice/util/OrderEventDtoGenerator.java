package com.mymicroservice.paymentservice.util;

import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;

public class OrderEventDtoGenerator {

    public static OrderEventDto generateOrderEventDto() {
        return OrderEventDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
    }
}
