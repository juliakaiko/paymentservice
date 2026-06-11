package com.mymicroservice.paymentservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaHeadersConstants {

    public static final String IDEMPOTENCE_ID = "X-Idempotence-Id";
    public static final String EVENT_TYPE = "X-Event-Type";
    public static final String TRACE_ID = "X-Trace-Id";
    public static final String SOURCE_SERVICE = "X-Source-Service";
    public static final String CREATE_PAYMENT_EVENT = "CREATE_PAYMENT";
}
