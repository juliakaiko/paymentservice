package com.mymicroservice.paymentservice.util;

import org.slf4j.MDC;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class KafkaMdcUtil {

    /**
     * Adds the requestId from the MDC to the Kafka headers
     */
    public static <T> Message<T> addMdcToMessage(T payload, String key, String topic) {
        MessageBuilder<T> builder = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key);

        // take the requestId from MDC and add it to the header
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isEmpty()) {
            builder.setHeader("X-Request-Id", requestId);
        }

        // add serviceName
        String serviceName = MDC.get("serviceName");
        if (serviceName != null && !serviceName.isEmpty()) {
            builder.setHeader("X-Source-Service", serviceName);
        }

        return builder.build();
    }

    /**
     * recovers MDC from Kafka headers
     */
    public static void restoreMdcFromMessage(Message<?> message) {
        Object requestId = message.getHeaders().get("X-Request-Id");
        if (requestId != null) {
            MDC.put("requestId", requestId.toString());
        }

        Object sourceService = message.getHeaders().get("X-Source-Service");
        if (sourceService != null) {
            MDC.put("sourceService", sourceService.toString());
        }
    }
}
