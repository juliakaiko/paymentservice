package com.mymicroservice.paymentservice.model;

import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbox_table")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxEvent {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID idempotenceId;
    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;
    private String sourceService;
    private String traceId;

    @Enumerated(EnumType.STRING)
    private InboxEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int retryCount;
}
