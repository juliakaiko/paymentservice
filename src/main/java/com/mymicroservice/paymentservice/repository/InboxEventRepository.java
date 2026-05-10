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
import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO inbox_table (
            event_id,
            idempotence_id,
            event_type,
            payload,
            source_service,
            request_id,
            status,
            created_at,
            processed_at
        )
        VALUES (
            :eventId,
            :idempotenceId,
            :eventType,
            CAST(:payload AS jsonb),
            :sourceService,
            :requestId,
            :status,
            :createdAt,
            :processedAt
        ) ON CONFLICT (idempotence_id) DO NOTHING""", nativeQuery = true)
    int insertIgnoreDuplicate(
            @Param("eventId") UUID eventId,
            @Param("idempotenceId") UUID idempotenceId,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("sourceService") String sourceService,
            @Param("requestId") String requestId,
            @Param("status") InboxEventStatus status,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Query("""
        UPDATE InboxEvent i
        SET i.status = :status,
            i.processedAt = :processedAt
        WHERE i.idempotenceId = :idempotenceId
    """)
    int updateStatus(
            @Param("status") InboxEventStatus status,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("idempotenceId") UUID idempotenceId
    );

    /**
     * Поиск с PESSIMISTIC_WRITE блокировкой, чтобы предотвратить race conditions
     * SELECT ... FOR UPDATE - блокирует строку до конца транзакции
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InboxEvent i WHERE i.idempotenceId = :idempotenceId")
    Optional<InboxEvent> findByIdempotenceIdForUpdate(@Param("idempotenceId") UUID id);
}
