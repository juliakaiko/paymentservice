package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;

import java.math.BigDecimal;

public class PaymentRequestDtoGenerator {

    public static PaymentRequestDto generatePaymentRequestDto() {
        return PaymentRequestDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(100000, 2))
                .build();
    }
}
