package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;

public class EventEnvelopeGenerator {

    public static final String DEFAULT_TRACE_ID = "test-trace-id";
    public static final String DEFAULT_SOURCE_SERVICE = "orderservice";
    public static final String DEFAULT_IDEMPOTENCE_ID = "test-idempotence-id";

    public static EventEnvelope<OrderEventDto> generateOrderEventEnvelope() {
        return generateOrderEventEnvelope(OrderEventDtoGenerator.generateOrderEventDto());
    }

    public static EventEnvelope<OrderEventDto> generateOrderEventEnvelope(OrderEventDto orderEventDto) {
        return new EventEnvelope<>(
                orderEventDto,
                DEFAULT_TRACE_ID,
                DEFAULT_SOURCE_SERVICE,
                DEFAULT_IDEMPOTENCE_ID
        );
    }

    public static EventEnvelope<PaymentEventDto> generatePaymentEventEnvelope() {
        return new EventEnvelope<>(
                PaymentEventDtoGenerator.generatePaymentEventDto(),
                DEFAULT_TRACE_ID,
                "paymentservice",
                DEFAULT_IDEMPOTENCE_ID
        );
    }
}
