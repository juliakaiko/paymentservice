package com.mymicroservice.paymentservice.unit.kafka;

import com.mymicroservice.paymentservice.kafka.inbox.InboxEventConsumer;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.service.InboxService;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TEST_IDEMPOTENCE_UUID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxEventConsumerTest {

    @InjectMocks
    private InboxEventConsumer inboxEventConsumer;

    @Mock
    private InboxService inboxService;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inboxEventConsumer, "serviceName", "paymentservice");
    }

    @Test
    void onCreateOrder_ShouldSaveInboxEventAndAck_WhenSerializationSucceeds() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        when(jsonMapper.toJson(event)).thenReturn(Optional.of("{\"orderId\":\"1\"}"));

        inboxEventConsumer.onCreateOrder(
                event, "key-1", 0, 42L,
                TEST_IDEMPOTENCE_UUID.toString(),
                CREATE_ORDER_EVENT_TYPE,
                TRACE_ID,
                SOURCE_SERVICE,
                acknowledgment);

        ArgumentCaptor<InboxEvent> captor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxService).saveInboxEvent(captor.capture());
        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(any(Duration.class));

        InboxEvent saved = captor.getValue();
        assertThat(saved.getIdempotenceId()).isEqualTo(TEST_IDEMPOTENCE_UUID);
        assertThat(saved.getStatus()).isEqualTo(InboxEventStatus.RECEIVED);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void onCreateOrder_ShouldSaveUnprocessableAndAck_WhenSerializationFails() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        when(jsonMapper.toJson(event)).thenReturn(Optional.empty());

        inboxEventConsumer.onCreateOrder(
                event, "key-1", 0, 42L,
                TEST_IDEMPOTENCE_UUID.toString(),
                CREATE_ORDER_EVENT_TYPE,
                TRACE_ID,
                SOURCE_SERVICE,
                acknowledgment);

        verify(inboxService).saveUnprocessableEvent(
                eq(TEST_IDEMPOTENCE_UUID),
                eq(CREATE_ORDER_EVENT_TYPE),
                eq(TRACE_ID),
                eq(SOURCE_SERVICE),
                eq("Failed to serialize OrderEventDto to JSON"));
        verify(inboxService, never()).saveInboxEvent(any());
        verify(acknowledgment).acknowledge();
        verify(acknowledgment, never()).nack(any(Duration.class));
    }

    @Test
    void onCreateOrder_ShouldNack_WhenSaveInboxEventThrows() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        when(jsonMapper.toJson(event)).thenReturn(Optional.of("{\"orderId\":\"1\"}"));
        doThrow(new RuntimeException("db error")).when(inboxService).saveInboxEvent(any());

        inboxEventConsumer.onCreateOrder(
                event, "key-1", 0, 42L,
                TEST_IDEMPOTENCE_UUID.toString(),
                CREATE_ORDER_EVENT_TYPE,
                TRACE_ID,
                SOURCE_SERVICE,
                acknowledgment);

        verify(acknowledgment).nack(Duration.ofMillis(100));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onCreateOrder_ShouldNack_WhenSaveUnprocessableEventThrows() {
        OrderEventDto event = OrderEventDtoGenerator.generateOrderEventDto();
        when(jsonMapper.toJson(event)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("db error"))
                .when(inboxService)
                .saveUnprocessableEvent(any(UUID.class), any(), any(), any(), any());

        inboxEventConsumer.onCreateOrder(
                event, "key-1", 0, 42L,
                TEST_IDEMPOTENCE_UUID.toString(),
                CREATE_ORDER_EVENT_TYPE,
                TRACE_ID,
                SOURCE_SERVICE,
                acknowledgment);

        verify(acknowledgment).nack(Duration.ofMillis(100));
        verify(acknowledgment, never()).acknowledge();
    }
}
