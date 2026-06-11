package com.mymicroservice.paymentservice.integration.repository;

import com.mymicroservice.paymentservice.configuration.AbstractContainerTest;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.util.InboxEventGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InboxEventRepositoryTest extends AbstractContainerTest {

    @Autowired
    private InboxEventRepository inboxEventRepository;

    private InboxEvent receivedEvent;

    @BeforeEach
    void init() {
        inboxEventRepository.deleteAll();

        receivedEvent = InboxEventGenerator.generateReceivedInboxEvent();
        receivedEvent = inboxEventRepository.save(receivedEvent);
    }

    @Test
    void countByStatus_shouldReturnCorrectCount_WhenEventsExist() {
        log.info("Test countByStatus - should return correct count when events exist");

        InboxEvent failedEvent = InboxEventGenerator.generateFailedInboxEvent(1);
        inboxEventRepository.save(failedEvent);

        long receivedCount = inboxEventRepository.countByStatus(InboxEventStatus.RECEIVED);
        long failedCount = inboxEventRepository.countByStatus(InboxEventStatus.FAILED);

        assertThat(receivedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    void insertIgnoreDuplicate_shouldInsertEvent_WhenIdempotenceIdIsUnique() {
        log.info("Test insertIgnoreDuplicate - should insert event when idempotenceId is unique");

        int inserted = inboxEventRepository.insertIgnoreDuplicate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                receivedEvent.getEventType(),
                receivedEvent.getPayload(),
                receivedEvent.getSourceService(),
                receivedEvent.getTraceId(),
                InboxEventStatus.RECEIVED.name(),
                LocalDateTime.now(),
                null,
                0
        );

        assertThat(inserted).isEqualTo(1);
        assertThat(inboxEventRepository.count()).isEqualTo(2);
    }

    @Test
    void insertIgnoreDuplicate_shouldIgnoreInsert_WhenIdempotenceIdAlreadyExists() {
        log.info("Test insertIgnoreDuplicate - should ignore insert when idempotenceId already exists");

        long beforeCount = inboxEventRepository.count();

        int inserted = inboxEventRepository.insertIgnoreDuplicate(
                UUID.randomUUID(),
                receivedEvent.getIdempotenceId(),
                receivedEvent.getEventType(),
                receivedEvent.getPayload(),
                receivedEvent.getSourceService(),
                receivedEvent.getTraceId(),
                InboxEventStatus.RECEIVED.name(),
                LocalDateTime.now(),
                null,
                0
        );

        long afterCount = inboxEventRepository.count();

        assertThat(inserted).isZero();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    void updateStatusAndRetryCount_shouldUpdateEvent_WhenIdempotenceIdExists() {
        log.info("Test updateStatusAndRetryCount - should update event when idempotenceId exists");

        LocalDateTime processedAt = LocalDateTime.now().withNano(0);

        int updated = inboxEventRepository.updateStatusAndRetryCount(
                InboxEventStatus.FAILED,
                processedAt,
                3,
                receivedEvent.getIdempotenceId()
        );

        InboxEvent updatedEvent = inboxEventRepository
                .findById(receivedEvent.getId())
                .orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(updatedEvent.getStatus()).isEqualTo(InboxEventStatus.FAILED);
        assertThat(updatedEvent.getRetryCount()).isEqualTo(3);
        assertThat(updatedEvent.getProcessedAt().withNano(0)).isEqualTo(processedAt);
    }

    @Test
    void updateStatusAndRetryCount_shouldReturnZero_WhenIdempotenceIdNotFound() {
        log.info("Test updateStatusAndRetryCount - should return zero when idempotenceId not found");

        int updated = inboxEventRepository.updateStatusAndRetryCount(
                InboxEventStatus.FAILED,
                LocalDateTime.now(),
                1,
                UUID.randomUUID()
        );

        assertThat(updated).isZero();
    }

    @Test
    void findEventsForProcessing_shouldReturnMatchingEvents_WhenStatusesExist() {
        log.info("Test findEventsForProcessing - should return matching events when statuses exist");

        InboxEvent failedEvent = InboxEventGenerator.generateFailedInboxEvent(2);
        inboxEventRepository.save(failedEvent);

        List<InboxEvent> result = inboxEventRepository.findEventsForProcessing(
                List.of(
                        InboxEventStatus.RECEIVED.name(),
                        InboxEventStatus.FAILED.name()
                ),
                10
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(InboxEvent::getStatus)
                .containsExactlyInAnyOrder(
                        InboxEventStatus.RECEIVED,
                        InboxEventStatus.FAILED
                );
    }

    @Test
    void findEventsForProcessing_shouldReturnFailedEvents_WhenFailedStatusRequested() {
        log.info("Test findEventsForProcessing - should return failed events when failed status requested");

        inboxEventRepository.deleteAll();

        InboxEvent failedEvent = InboxEventGenerator.generateFailedInboxEvent(3);
        inboxEventRepository.save(failedEvent);

        List<InboxEvent> result = inboxEventRepository.findEventsForProcessing(
                List.of(InboxEventStatus.FAILED.name()),
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(InboxEventStatus.FAILED);
        assertThat(result.get(0).getRetryCount()).isEqualTo(3);
    }

    @Test
    void findEventsForProcessing_shouldReturnEmptyList_WhenNoMatchingStatuses() {
        log.info("Test findEventsForProcessing - should return empty list when no matching statuses");

        List<InboxEvent> result = inboxEventRepository.findEventsForProcessing(
                List.of(InboxEventStatus.PROCESSED.name()),
                10
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findEventsForProcessing_shouldReturnLimitedNumberOfEvents_WhenLimitIsSpecified() {
        log.info("Test findEventsForProcessing - should return limited number of events when limit is specified");

        inboxEventRepository.save(InboxEventGenerator.generateReceivedInboxEvent());
        inboxEventRepository.save(InboxEventGenerator.generateReceivedInboxEvent());

        List<InboxEvent> result = inboxEventRepository.findEventsForProcessing(
                List.of(InboxEventStatus.RECEIVED.name()),
                2
        );

        assertThat(result).hasSize(2);
    }
}