package com.mymicroservice.paymentservice.util;

import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.mymicroservices.common.events.OrderEventDto;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.mymicroservice.paymentservice.util.CommonConstants.EVENT_TYPE;
import static com.mymicroservice.paymentservice.util.CommonConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.paymentservice.util.CommonConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.CommonConstants.TRACE_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.CREATE_ORDER_TOPIC;

@UtilityClass
public class KafkaTestMessageSender {

    public static void sendOrderEvent(KafkaTemplate<String, OrderEventDto> kafkaTemplate,
                                      OrderEventDto event,
                                      UUID idempotenceId,
                                      String eventType,
                                      String traceId,
                                      String sourceService) {
        ProducerRecord<String, OrderEventDto> record =
                new ProducerRecord<>(CREATE_ORDER_TOPIC, event.getOrderId(), event);
        record.headers().add(IDEMPOTENCE_ID, idempotenceId.toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EVENT_TYPE, eventType.getBytes(StandardCharsets.UTF_8));
        record.headers().add(TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(SOURCE_SERVICE, sourceService.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }
}
