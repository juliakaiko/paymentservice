package com.mymicroservice.paymentservice.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Status {

    PAID ("PAID"),
    FAILED("FAILED");

    private final String status;
}
