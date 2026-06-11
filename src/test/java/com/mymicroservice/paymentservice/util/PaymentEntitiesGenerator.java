package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentEntitiesGenerator {

    public static List<Payment> generatePaymentEntities() {
        Payment payment1 = Payment.builder()
                .id("test-payment-1")
                .userId("1")
                .orderId("1")
                .status(PaymentStatus.PAID)
                .timestamp(LocalDateTime.of(2025, 1, 1, 1, 10, 1))
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();

        Payment payment2 = Payment.builder()
                .id("test-payment-2")
                .userId("2")
                .orderId("2")
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.of(2025, 2, 2, 2, 20, 2))
                .paymentAmount(BigDecimal.valueOf(2000.00))
                .build();

        return  List.of(payment1, payment2);
    }
}
