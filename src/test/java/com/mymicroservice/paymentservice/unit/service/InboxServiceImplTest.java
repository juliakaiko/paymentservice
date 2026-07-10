package com.mymicroservice.paymentservice.unit.service;

import com.mymicroservice.paymentservice.alert.InboxDeadLetterAlert;
import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import com.mymicroservice.paymentservice.metrics.InboxMetrics;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.repository.InboxEventRepository;
import com.mymicroservice.paymentservice.service.PaymentService;
import com.mymicroservice.paymentservice.service.impl.InboxServiceImpl;
import com.mymicroservice.paymentservice.util.InboxEventGenerator;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceImplTest {

    @InjectMocks
    private InboxServiceImpl inboxService;

    @Mock
    private InboxDeadLetterAlert deadLetterAlert;

    @Mock
    private InboxEventRepository inboxRepository;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private PaymentService paymentService;

    private InboxMetrics inboxMetrics;

    @BeforeEach
    void setUp() {
        inboxMetrics = new InboxMetrics(new SimpleMeterRegistry(), inboxRepository);
        ReflectionTestUtils.invokeMethod(inboxMetrics, "registerMeters");
        ReflectionTestUtils.setField(inboxService, "inboxMetrics", inboxMetrics);
        ReflectionTestUtils.setField(inboxService, "batchSize", 100);
        ReflectionTestUtils.setField(inboxService, "maxRetries", 10);
    }

    @Test
    void saveInboxEvent_ShouldInsertEvent_WhenEventIsNew() {
        InboxEvent event = InboxEventGenerator.generateReceivedInboxEvent();
        when(inboxRepository.insertIgnoreDuplicate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(1);

        inboxService.saveInboxEvent(event);

        verify(inboxRepository).insertIgnoreDuplicate(
                eq(event.getId()),
                eq(event.getIdempotenceId()),
                eq(event.getEventType()),
                eq(event.getPayload()),
                eq(event.getSourceService()),
                eq(event.getTraceId()),
                eq(event.getStatus().name()),
                eq(event.getCreatedAt()),
                eq(event.getProcessedAt()),
                eq(event.getRetryCount())
        );
    }

    @Test
    void saveInboxEvent_ShouldIgnoreDuplicate_WhenEventAlreadyExists() {
        InboxEvent event = InboxEventGenerator.generateReceivedInboxEvent();
        when(inboxRepository.insertIgnoreDuplicate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(0);

        inboxService.saveInboxEvent(event);

        verify(inboxRepository).insertIgnoreDuplicate(
                eq(event.getId()),
                eq(event.getIdempotenceId()),
                eq(event.getEventType()),
                eq(event.getPayload()),
                eq(event.getSourceService()),
                eq(event.getTraceId()),
                eq(event.getStatus().name()),
                eq(event.getCreatedAt()),
                eq(event.getProcessedAt()),
                eq(event.getRetryCount())
        );
    }

    @Test
    void processPendingInboxEvents_ShouldMarkProcessed_WhenPaymentCreatedSuccessfully() {
        InboxEvent event = InboxEventGenerator.generateReceivedInboxEvent();
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto();

        when(inboxRepository.findEventsForProcessing(any(), anyInt())).thenReturn(List.of(event));
        when(jsonMapper.fromJson(event.getPayload(), OrderEventDto.class)).thenReturn(Optional.of(orderEventDto));
        when(paymentService.createPayment(any(EventEnvelope.class)))
                .thenReturn(PaymentEventDto.builder().id("payment-1").orderId("1").build());
        when(inboxRepository.updateStatusAndRetryCount(any(), any(), anyInt(), any())).thenReturn(1);

        inboxService.processPendingInboxEvents();

        verify(paymentService).createPayment(any(EventEnvelope.class));
        verify(inboxRepository).updateStatusAndRetryCount(
                eq(InboxEventStatus.PROCESSED), any(), eq(0), eq(event.getIdempotenceId()));
    }

    @Test
    void processPendingInboxEvents_ShouldIncrementRetryCount_WhenProcessingFails() {
        InboxEvent event = InboxEventGenerator.generateReceivedInboxEvent();

        when(inboxRepository.findEventsForProcessing(any(), anyInt())).thenReturn(List.of(event));
        when(jsonMapper.fromJson(event.getPayload(), OrderEventDto.class))
                .thenThrow(new IllegalStateException("deserialize error"));
        when(inboxRepository.updateStatusAndRetryCount(any(), any(), anyInt(), any())).thenReturn(1);

        inboxService.processPendingInboxEvents();

        verify(inboxRepository).updateStatusAndRetryCount(
                eq(InboxEventStatus.FAILED), any(), eq(1), eq(event.getIdempotenceId()));
        verify(deadLetterAlert, never()).alert(any(), any(), any());
    }

    @Test
    void processPendingInboxEvents_ShouldMarkDead_WhenMaxRetriesExceeded() {
        InboxEvent event = InboxEventGenerator.generateFailedInboxEvent(9);

        when(inboxRepository.findEventsForProcessing(any(), anyInt())).thenReturn(List.of(event));
        when(jsonMapper.fromJson(event.getPayload(), OrderEventDto.class))
                .thenThrow(new IllegalStateException("processing error"));
        when(inboxRepository.updateStatusAndRetryCount(any(), any(), anyInt(), any())).thenReturn(1);

        inboxService.processPendingInboxEvents();

        verify(inboxRepository).updateStatusAndRetryCount(
                eq(InboxEventStatus.DEAD), any(), eq(10), eq(event.getIdempotenceId()));
        verify(deadLetterAlert).alert(eq(event), eq("Max retries exceeded"), any());
    }

    @Test
    void saveUnprocessableEvent_ShouldSaveDeadEvent_WhenPayloadCannotBeSerialized() {
        UUID idempotenceId = UUID.randomUUID();
        when(inboxRepository.insertIgnoreDuplicate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(1);

        inboxService.saveUnprocessableEvent(
                idempotenceId,
                "CREATE_ORDER",
                "trace-id",
                "orderservice",
                "Failed to serialize OrderEventDto to JSON"
        );

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(inboxRepository).insertIgnoreDuplicate(
                any(), eq(idempotenceId), any(), any(), any(), any(),
                statusCaptor.capture(), any(), any(), eq(10));

        assertEquals(InboxEventStatus.DEAD.name(), statusCaptor.getValue());
        verify(deadLetterAlert, atLeastOnce()).alert(any(), any(), any());
    }

    @Test
    void saveUnprocessableEvent_ShouldIgnoreDuplicate_WhenEventAlreadyExists() {
        UUID idempotenceId = UUID.randomUUID();
        when(inboxRepository.insertIgnoreDuplicate(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(0);

        inboxService.saveUnprocessableEvent(
                idempotenceId,
                "CREATE_ORDER",
                "trace-id",
                "orderservice",
                "Failed to serialize OrderEventDto to JSON"
        );

        verify(deadLetterAlert, never()).alert(any(), any(), any());
    }

    @Test
    void processPendingInboxEvents_ShouldHandleDeadUpdateFailure_WhenMaxRetriesExceededAndUpdateFails() {
        InboxEvent event = InboxEventGenerator.generateFailedInboxEvent(9);

        when(inboxRepository.findEventsForProcessing(any(), anyInt())).thenReturn(List.of(event));
        when(jsonMapper.fromJson(event.getPayload(), OrderEventDto.class))
                .thenThrow(new IllegalStateException("processing error"));
        when(inboxRepository.updateStatusAndRetryCount(any(), any(), anyInt(), any())).thenReturn(0);

        inboxService.processPendingInboxEvents();

        verify(deadLetterAlert, never()).alert(any(), any(), any());
    }
}
