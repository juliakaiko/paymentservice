package com.mymicroservice.paymentservice.repository;

import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    long countByStatus(InboxEventStatus status);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO inbox_table (
            id,
            idempotence_id,
            event_type,
            payload,
            source_service,
            trace_id,
            status,
            created_at,
            processed_at,
            retry_count
        )
        VALUES (
            :id,
            :idempotenceId,
            :eventType,
            CAST(:payload AS jsonb),
            :sourceService,
            :traceId,
            :status,
            :createdAt,
            :processedAt,
            :retryCount
        ) ON CONFLICT (idempotence_id) DO NOTHING""", nativeQuery = true)
    int insertIgnoreDuplicate(
            @Param("id") UUID id,
            @Param("idempotenceId") UUID idempotenceId,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("sourceService") String sourceService,
            @Param("traceId") String traceId,
            @Param("status") String status,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("retryCount") int retryCount
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE InboxEvent i
        SET i.status = :status,
            i.processedAt = :processedAt,
            i.retryCount = :retryCount
        WHERE i.idempotenceId = :idempotenceId
    """)
    int updateStatusAndRetryCount(
            @Param("status") InboxEventStatus status,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("retryCount") int retryCount,
            @Param("idempotenceId") UUID idempotenceId
    );

    @Query(value = """
        SELECT *
        FROM inbox_table
        WHERE status IN (:statuses)
        ORDER BY created_at
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
        """, nativeQuery = true)
    List<InboxEvent> findEventsForProcessing(
            @Param("statuses") List<String> statuses,
            @Param("limit") int limit
    );
}
