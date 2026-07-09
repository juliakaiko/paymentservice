package com.mymicroservice.paymentservice.unit.util;

import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.util.MdcUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void runWithInboxEvent_ShouldSetAndRestoreMdc_WhenEventHasTraceAndSource() {
        MDC.put("traceId", "old-trace");
        MDC.put("serviceName", "old-service");
        InboxEvent event = InboxEvent.builder()
                .id(UUID.randomUUID())
                .traceId("new-trace")
                .sourceService("paymentservice")
                .build();
        AtomicReference<String> traceDuringAction = new AtomicReference<>();
        AtomicReference<String> serviceDuringAction = new AtomicReference<>();

        MdcUtils.runWithInboxEvent(event, () -> {
            traceDuringAction.set(MDC.get("traceId"));
            serviceDuringAction.set(MDC.get("serviceName"));
        });

        assertEquals("new-trace", traceDuringAction.get());
        assertEquals("paymentservice", serviceDuringAction.get());
        assertEquals("old-trace", MDC.get("traceId"));
        assertEquals("old-service", MDC.get("serviceName"));
    }

    @Test
    void runWithInboxEvent_ShouldRemoveMdcKeys_WhenPreviousValuesWereNull() {
        InboxEvent event = InboxEvent.builder()
                .id(UUID.randomUUID())
                .traceId("trace-1")
                .sourceService("paymentservice")
                .build();

        MdcUtils.runWithInboxEvent(event, () -> {
        });

        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("serviceName"));
    }
}
