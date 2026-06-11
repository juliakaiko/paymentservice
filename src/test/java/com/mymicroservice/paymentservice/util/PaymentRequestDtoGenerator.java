package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;

import java.math.BigDecimal;

import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;

public class PaymentRequestDtoGenerator {

    public static PaymentRequestDto generatePaymentRequestDto() {
        return PaymentRequestDto.builder()
                .orderId(ENTITY_ID)
                .userId(ENTITY_ID)
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();
    }
}
