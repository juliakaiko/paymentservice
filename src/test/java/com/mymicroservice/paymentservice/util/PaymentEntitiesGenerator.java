package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAYMENT_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_PAYMENT_ID;

public class PaymentEntitiesGenerator {

    public static List<Payment> generatePaymentEntities() {
        Payment payment1 = Payment.builder()
                .id(PAYMENT_ID)
                .userId(ENTITY_ID)
                .orderId(ENTITY_ID)
                .status(PaymentStatus.PAID)
                .timestamp(LocalDateTime.of(2025, 1, 1, 1, 10, 1))
                .paymentAmount(BigDecimal.valueOf(1000.00))
                .build();

        Payment payment2 = Payment.builder()
                .id(SECOND_PAYMENT_ID)
                .userId(SECOND_ENTITY_ID)
                .orderId(SECOND_ENTITY_ID)
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.of(2025, 2, 2, 2, 20, 2))
                .paymentAmount(BigDecimal.valueOf(2000.00))
                .build();

        return  List.of(payment1, payment2);
    }
}
