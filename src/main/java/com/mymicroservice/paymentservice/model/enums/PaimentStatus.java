package com.mymicroservice.paymentservice.model.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaimentStatus {

    PAID ("PAID"),
    FAILED("FAILED");

    private final String status;
}
