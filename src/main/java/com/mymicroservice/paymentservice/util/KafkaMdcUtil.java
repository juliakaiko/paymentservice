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

}
