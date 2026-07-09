package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.model.InboxEvent;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

@UtilityClass
public class MdcUtils {

    public static void runWithInboxEvent(InboxEvent event, Runnable action) {
        String oldTraceId = MDC.get("traceId");
        String oldService = MDC.get("serviceName");

        try {
            if (event.getTraceId() != null) {
                MDC.put("traceId", event.getTraceId());
            }

            if (event.getSourceService() != null) {
                MDC.put("serviceName", event.getSourceService());
            }

            action.run();
        } finally {
            restore(oldTraceId, oldService);
        }
    }

    private static void restore(String traceId, String serviceName) {
        if (traceId != null) {
            MDC.put("traceId", traceId);
        } else {
            MDC.remove("traceId");
        }

        if (serviceName != null) {
            MDC.put("serviceName", serviceName);
        } else {
            MDC.remove("serviceName");
        }
    }
}