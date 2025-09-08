package com.mymicroservices.paymentservice.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Status {

    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String status;
}
