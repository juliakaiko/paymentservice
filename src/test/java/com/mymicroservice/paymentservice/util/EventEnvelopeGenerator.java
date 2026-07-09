package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;

import static com.mymicroservice.paymentservice.util.data.TestConstants.IDEMPOTENCE_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SOURCE_SERVICE;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SERVICE_NAME;
import static com.mymicroservice.paymentservice.util.data.TestConstants.TRACE_ID;

public class EventEnvelopeGenerator {

    public static EventEnvelope<OrderEventDto> generateOrderEventEnvelope() {
        return generateOrderEventEnvelope(OrderEventDtoGenerator.generateOrderEventDto());
    }

    public static EventEnvelope<OrderEventDto> generateOrderEventEnvelope(OrderEventDto orderEventDto) {
        return new EventEnvelope<>(
                orderEventDto,
                TRACE_ID,
                SOURCE_SERVICE,
                IDEMPOTENCE_ID
        );
    }

    public static EventEnvelope<PaymentEventDto> generatePaymentEventEnvelope() {
        return new EventEnvelope<>(
                PaymentEventDtoGenerator.generatePaymentEventDto(),
                TRACE_ID,
                SERVICE_NAME,
                IDEMPOTENCE_ID
        );
    }
}
