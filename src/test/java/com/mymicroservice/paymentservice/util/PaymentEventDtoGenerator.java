package com.mymicroservice.paymentservice.util;

import org.mymicroservices.common.events.PaymentEventDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.FAILED_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAID_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAYMENT_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_PAYMENT_ID;

public class PaymentEventDtoGenerator {

    public static PaymentEventDto generatePaymentEventDto() {
        return PaymentEventDto.builder()
                .id(PAYMENT_ID)
                .orderId(ENTITY_ID)
                .userId(ENTITY_ID)
                .status(PAID_STATUS )
                .timestamp(LocalDateTime.of(2025, 1, 1, 1, 10, 1).toString())
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
    }

    public static PaymentEventDto generateSecondPaymentEventDto() {
        return PaymentEventDto.builder()
                .id(SECOND_PAYMENT_ID)
                .orderId(SECOND_ENTITY_ID)
                .userId(SECOND_ENTITY_ID)
                .status(FAILED_STATUS)
                .timestamp(LocalDateTime.of(2025, 2, 2, 2, 20, 2).toString())
                .paymentAmount(BigDecimal.valueOf(2000.00))
                .build();
    }
}
