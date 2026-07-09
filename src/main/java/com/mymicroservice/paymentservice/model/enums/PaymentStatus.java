package com.mymicroservice.paymentservice.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaymentStatus {

    PAID ("PAID"),
    FAILED("FAILED");

    private final String status;
}
