package com.mymicroservice.paymentservice.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.paymentservice.alert.InboxDeadLetterAlert;
import com.mymicroservice.paymentservice.exception.InboxEventNotFoundException;
import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import com.mymicroservice.paymentservice.metrics.InboxMetrics;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.service.InboxService;
import com.mymicroservice.paymentservice.service.PaymentService;
import com.mymicroservice.paymentservice.util.MdcUtils;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxServiceImpl implements InboxService {

    private final InboxDeadLetterAlert deadLetterAlert;
    private final InboxEventRepository inboxRepository;
    private final InboxMetrics inboxMetrics;
    private final JsonMapper jsonMapper;
    private final PaymentService paymentService;

    @Value("${inbox.batch-size:100}")
    private int batchSize;

    @Value("${inbox.max-retries:10}")
    private int maxRetries;

    @Override
    @Transactional
    public void saveInboxEvent(InboxEvent event) {
        int inserted = insertEvent(event);
        if (inserted == 0) {
            log.warn("Duplicate inbox event ignored. id={}", event.getIdempotenceId());
        }
    }

    @Override
    @Transactional
    public void saveUnprocessableEvent(UUID idempotenceId, String eventType, String traceId,
                                       String sourceService, String errorMessage) {
        String payload = "{\"error\":\"" + errorMessage.replace("\"", "\\\"") + "\"}";

        InboxEvent poisonEvent = InboxEvent.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .idempotenceId(idempotenceId)
                .eventType(eventType)
                .payload(payload)
                .sourceService(sourceService)
                .traceId(traceId)
                .status(InboxEventStatus.DEAD)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .retryCount(maxRetries)
                .build();

        int inserted = insertEvent(poisonEvent);
        if (inserted == 0) {
            log.warn("Unprocessable inbox event already exists. id={}", idempotenceId);
            return;
        }

        inboxMetrics.recordDeadLetter();
        deadLetterAlert.alert(poisonEvent, errorMessage, null);
        log.warn("Saved unprocessable inbox event as DEAD. id={}", idempotenceId);
    }

    @Override
    @Transactional
    public void processPendingInboxEvents() {
        List<InboxEvent> events = inboxRepository.findEventsForProcessing(
                List.of(InboxEventStatus.RECEIVED.name(), InboxEventStatus.FAILED.name()),
                batchSize
        );

        for (InboxEvent event : events) {
            Timer.Sample sample = inboxMetrics.startProcessingTimer();
            try {
                MdcUtils.runWithInboxEvent(event, () -> {
                    OrderEventDto dto = jsonMapper.fromJson(event.getPayload(), OrderEventDto.class)
                            .orElseThrow(() -> new IllegalStateException("Failed to deserialize inbox payload"));

                    EventEnvelope<OrderEventDto> eventEnvelope = new EventEnvelope<>(
                            dto,
                            event.getTraceId(),
                            event.getSourceService(),
                            event.getId().toString()
                    );
                    paymentService.createPayment(eventEnvelope);

                    updateStatusAndRetryCount(event.getIdempotenceId(), InboxEventStatus.PROCESSED, event.getRetryCount());
                    inboxMetrics.recordProcessed();

                    log.info("Inbox event processed successfully. id={}", event.getIdempotenceId());
                });
            } catch (Exception ex) {
                handleProcessingFailure(event, ex);
            } finally {
                inboxMetrics.recordProcessingDuration(sample);
            }
        }
    }

    private void updateStatusAndRetryCount(UUID idempotenceId, InboxEventStatus status, int retryCount) {
        int updated = inboxRepository.updateStatusAndRetryCount(
                status, LocalDateTime.now(), retryCount, idempotenceId);
        if (updated == 0) {
            throw new InboxEventNotFoundException("Inbox event not found: " + idempotenceId);
        }
        log.info("Inbox status updated: idempotenceId={}, status={}, retryCount={}",
                idempotenceId, status, retryCount);
    }

    private void handleProcessingFailure(InboxEvent event, Exception ex) {
        int newRetryCount = event.getRetryCount() + 1;
        log.error("Failed to process inbox event. id={}, retryCount={}",
                event.getIdempotenceId(), newRetryCount, ex);

        if (newRetryCount >= maxRetries) {
            try {
                updateStatusAndRetryCount(event.getIdempotenceId(), InboxEventStatus.DEAD, newRetryCount);
                inboxMetrics.recordDeadLetter();
                deadLetterAlert.alert(event, "Max retries exceeded", ex);
            } catch (Exception updateEx) {
                log.error("Failed to mark inbox event as DEAD. id={}", event.getIdempotenceId(), updateEx);
            }
        } else {
            try {
                updateStatusAndRetryCount(event.getIdempotenceId(), InboxEventStatus.FAILED, newRetryCount);
                inboxMetrics.recordFailed();
            } catch (Exception updateEx) {
                log.error("Failed to update inbox event retry status. id={}", event.getIdempotenceId(), updateEx);
            }
        }
    }

    private int insertEvent(InboxEvent event) {
        return inboxRepository.insertIgnoreDuplicate(
                event.getId(),
                event.getIdempotenceId(),
                event.getEventType(),
                event.getPayload(),
                event.getSourceService(),
                event.getTraceId(),
                event.getStatus().name(),
                event.getCreatedAt(),
                event.getProcessedAt(),
                event.getRetryCount()
        );
    }
}
