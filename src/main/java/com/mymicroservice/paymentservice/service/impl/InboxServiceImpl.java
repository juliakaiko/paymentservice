package com.mymicroservice.paymentservice.service.impl;

import com.mymicroservice.paymentservice.exception.InboxEventNotFound;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.service.InboxService;
import com.mymicroservice.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxServiceImpl implements InboxService {

    private final InboxEventRepository inboxRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(InboxEvent inboxEvent, OrderEventDto event, String idempotenceId) {

        int inserted = inboxRepository.insertIgnoreDuplicate(
                inboxEvent.getEventId(),
                inboxEvent.getIdempotenceId(),
                inboxEvent.getEventType(),
                inboxEvent.getPayload(),
                inboxEvent.getSourceService(),
                inboxEvent.getRequestId(),
                inboxEvent.getStatus(),
                inboxEvent.getCreatedAt(),
                inboxEvent.getProcessedAt()
        );
        if (inserted == 0) {
            log.info("Duplicate event ignored: {}", idempotenceId);
            return;
        }
        PaymentEventDto saved = paymentService.createPayment(event);

        log.info("Successfully saved payment: {}", saved);

        updateStatus(InboxEventStatus.PROCESSED, idempotenceId);
    }

    @Override
    @Transactional
    public void updateStatus(InboxEventStatus status, String idempotenceId) {
        int updated = inboxRepository.updateStatus(status, LocalDateTime.now(), UUID.fromString(idempotenceId));
        if (updated == 0) {
            throw new InboxEventNotFound("Inbox event not found: " + idempotenceId);
        }
        log.info("Inbox status updated: idempotenceId={}, status={}", idempotenceId, status);
    }
}
