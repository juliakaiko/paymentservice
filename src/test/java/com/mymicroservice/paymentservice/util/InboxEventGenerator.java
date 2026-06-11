package com.mymicroservice.paymentservice.util;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class InboxEventGenerator {

    public static final String DEFAULT_PAYLOAD =
            "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":1000.00}";

    public static InboxEvent generateReceivedInboxEvent() {
        return generateInboxEvent(InboxEventStatus.RECEIVED, 0, DEFAULT_PAYLOAD);
    }

    public static InboxEvent generateFailedInboxEvent(int retryCount) {
        return generateInboxEvent(InboxEventStatus.FAILED, retryCount, DEFAULT_PAYLOAD);
    }

    public static InboxEvent generateInboxEvent(InboxEventStatus status, int retryCount, String payload) {
        UUID idempotenceId = UuidCreator.getTimeOrderedEpoch();
        return InboxEvent.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .idempotenceId(idempotenceId)
                .eventType("CREATE_ORDER")
                .payload(payload)
                .sourceService("orderservice")
                .traceId("test-trace-id")
                .status(status)
                .createdAt(LocalDateTime.now())
                .processedAt(null)
                .retryCount(retryCount)
                .build();
    }
}
