package com.mymicroservice.paymentservice.util;

import org.mymicroservices.common.events.PaymentEventDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentEventDtoGenerator {

    public static PaymentEventDto generatePaymentEventDto() {
        return PaymentEventDto.builder()
                .id("test-payment-1")
                .orderId("1")
                .userId("1")
                .status("PAID")
                .timestamp(LocalDateTime.of(2025, 1, 1, 1, 10, 1).toString())
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
    }
}
