package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentEntitiesGenerator {

    public static List<PaymentEntity> generatePaymentEntities() {
        PaymentEntity paymentEntity1 = PaymentEntity.builder()
                .id("test-payment-1")
                .userId("1")
                .orderId("1")
                .status(Status.PAID)
                .timestamp(LocalDateTime.of(2025, 1, 1, 1, 10, 1))
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();

        PaymentEntity paymentEntity2 = PaymentEntity.builder()
                .id("test-payment-2")
                .userId("2")
                .orderId("2")
                .status(Status.FAILED)
                .timestamp(LocalDateTime.of(2025, 2, 2, 2, 20, 2))
                .paymentAmount(BigDecimal.valueOf(2000.00))
                .build();

        return  List.of(paymentEntity1,paymentEntity2);
    }
}
