package com.mymicroservice.paymentservice.kafka.inbox;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mymicroservice.paymentservice.util.KafkaHeadersConstants.EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.KafkaHeadersConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.paymentservice.util.KafkaHeadersConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.KafkaHeadersConstants.TRACE_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxEventConsumer {

    private final InboxService inboxService;
    private final JsonMapper jsonMapper;

    @Value("${spring.application.name}")
    private String serviceName;

    @KafkaListener(
            topics = "${kafka.consumer.topics.create-order}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void onCreateOrder(
            @Payload OrderEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = IDEMPOTENCE_ID) String idempotenceId,
            @Header(value = EVENT_TYPE) String eventType,
            @Header(value = TRACE_ID) String traceId,
            @Header(value = SOURCE_SERVICE) String sourceService,
            Acknowledgment ack) {

        jsonMapper.toJson(event)
                .ifPresentOrElse(payloadJson -> {
                    try {
                        setUpMDC(traceId, sourceService);

                        InboxEvent inboxEvent = InboxEvent.builder()
                                .id(UuidCreator.getTimeOrderedEpoch())
                                .idempotenceId(UUID.fromString(idempotenceId))
                                .eventType(eventType)
                                .payload(payloadJson)
                                .sourceService(sourceService)
                                .traceId(traceId)
                                .status(InboxEventStatus.RECEIVED)
                                .createdAt(LocalDateTime.now())
                                .processedAt(null)
                                .retryCount(0)
                                .build();

                        log.info("Received CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                                key, partition, offset, event);

                        inboxService.saveInboxEvent(inboxEvent);
                        ack.acknowledge();

                        log.info("Inbox event saved for orderId: {}", event.getOrderId());
                    } catch (Exception e) {
                        log.error("Error saving CREATE_ORDER event to inbox [key: {}, partition: {}, offset: {}]",
                                key, partition, offset, e);
                        ack.nack(Duration.ofMillis(100));
                    } finally {
                        MDC.clear();
                    }
                }, () -> {
                    log.error("Failed to serialize inbox payload. eventId={}", idempotenceId);
                    try {
                        setUpMDC(traceId, sourceService);
                        inboxService.saveUnprocessableEvent(
                                UUID.fromString(idempotenceId),
                                eventType,
                                traceId,
                                sourceService,
                                "Failed to serialize OrderEventDto to JSON"
                        );
                        ack.acknowledge();
                    } catch (Exception e) {
                        log.error("Failed to persist unprocessable inbox event. eventId={}", idempotenceId, e);
                        ack.nack(Duration.ofMillis(100));
                    } finally {
                        MDC.clear();
                    }
                });
    }

    private void setUpMDC(String traceId, String sourceService) {
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        if (sourceService != null) {
            MDC.put("sourceService", sourceService);
        }
        MDC.put("serviceName", serviceName);
    }
}
