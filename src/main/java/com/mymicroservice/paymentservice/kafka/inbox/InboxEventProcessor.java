package com.mymicroservice.paymentservice.kafka.inbox;

import com.github.f4b6a3.uuid.UuidCreator;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import com.mymicroservice.paymentservice.model.InboxEvent;
import com.mymicroservice.paymentservice.model.enums.InboxEventStatus;
import com.mymicroservice.paymentservice.service.InboxService;
import com.mymicroservice.paymentservice.service.PaymentService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxEventProcessor {

    private final InboxService inboxService;
    private final JsonMapper jsonMapper;

    private static final String IDEMPOTENCE_ID_HEADER = "X-Idempotence-Id";
    private static final String EVENT_TYPE = "X-Event-Type";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";

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
            @Header(value = IDEMPOTENCE_ID_HEADER) String idempotenceId,
            @Header(value = EVENT_TYPE) String eventType,
            @Header(value = REQUEST_ID_HEADER) String requestId,
            @Header(value = SOURCE_SERVICE_HEADER) String sourceService,
            Acknowledgment ack) {

        setUpMDC(requestId, sourceService);

        InboxEvent inboxEvent = InboxEvent.builder()
                .eventId(UuidCreator.getTimeOrderedEpoch())
                .idempotenceId(UUID.fromString(idempotenceId))
                .eventType(eventType)
                .payload(jsonMapper.toJson(event))
                .sourceService(sourceService)
                .requestId(requestId)
                .status(InboxEventStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .processedAt(null)
                .build();

        try {
            log.info("Received CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}", key, partition, offset, event);

            inboxService.process(inboxEvent, event, idempotenceId);

            ack.acknowledge();

            log.info("Successfully processed payment for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing CREATE_ORDER event [key: {}, partition: {}, offset: {}]: {}",
                    key, partition, offset, e.getMessage());

            ack.nack(Duration.ofMillis(100));
        } finally {
            MDC.clear();
        }
    }

    private void setUpMDC(String requestId, String sourceService) {
        if (requestId != null)
            MDC.put("requestId", requestId);
        if (sourceService != null)
            MDC.put("sourceService", sourceService);
        MDC.put("serviceName", serviceName);
    }
}
