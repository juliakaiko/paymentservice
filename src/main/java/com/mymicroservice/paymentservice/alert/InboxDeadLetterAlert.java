package com.mymicroservice.paymentservice.alert;

import com.mymicroservice.paymentservice.model.InboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InboxDeadLetterAlert {

    public void alert(InboxEvent event, String reason, Throwable cause) {
        log.error(
                "INBOX_DEAD_LETTER_ALERT: event moved to DEAD. "
                        + "idempotenceId={}, eventType={}, traceId={}, retryCount={}, reason={}",
                event.getIdempotenceId(),
                event.getEventType(),
                event.getTraceId(),
                event.getRetryCount(),
                reason,
                cause
        );
    }
}
