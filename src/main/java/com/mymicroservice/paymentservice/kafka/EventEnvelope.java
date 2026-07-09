package com.mymicroservice.paymentservice.kafka;

public record EventEnvelope<T>(
        T payload,
        String traceId,
        String sourceService,
        String idempotenceId
) {}
