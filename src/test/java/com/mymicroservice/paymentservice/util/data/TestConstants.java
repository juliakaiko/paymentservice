package com.mymicroservice.paymentservice.util.data;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestConstants {

    public static final String SOURCE_SERVICE = "orderservice";
    public static final String SERVICE_NAME = "paymentservice";

    public static final String ENTITY_ID = "1";
    public static final String SECOND_ENTITY_ID = "2";
    public static final String PAYMENT_ID = "test-payment-1";
    public static final String SECOND_PAYMENT_ID = "test-payment-2";
    public static final String TRACE_ID = "test-trace-id";
    public static final String IDEMPOTENCE_ID = "test-idempotence-id";
    public static final String NON_EXISTENT_ID = "non-existent";

    public static final String CREATE_ORDER_EVENT_TYPE = "CREATE_ORDER";
    public static final String PAID_STATUS = "PAID";
    public static final String FAILED_STATUS = "FAILED";

}
