package com.mymicroservice.paymentservice.service;

import com.mymicroservice.paymentservice.model.InboxEvent;

import java.util.UUID;

public interface InboxService {

    void saveInboxEvent(InboxEvent event);

    void saveUnprocessableEvent(UUID idempotenceId, String eventType, String traceId,
                                String sourceService, String errorMessage);

    void processPendingInboxEvents();
}
