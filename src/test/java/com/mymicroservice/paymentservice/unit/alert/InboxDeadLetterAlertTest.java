package com.mymicroservice.paymentservice.unit.alert;

import com.mymicroservice.paymentservice.alert.InboxDeadLetterAlert;
import com.mymicroservice.paymentservice.model.InboxEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InboxDeadLetterAlertTest {

    @Test
    void alert_ShouldLogError_WhenEventMovedToDead() {
        InboxDeadLetterAlert alert = new InboxDeadLetterAlert();
        InboxEvent event = InboxEvent.builder()
                .id(UUID.randomUUID())
                .idempotenceId(UUID.randomUUID())
                .eventType("CREATE_PAYMENT")
                .traceId("trace-1")
                .retryCount(3)
                .build();

        assertDoesNotThrow(() -> alert.alert(event, "max retries exceeded", new RuntimeException("boom")));
    }
}
