package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.util.PaymentRequestDtoGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentRequestMapperTest {

    @Test
    void toOrderEventDto_ShouldMapFieldsCorrectly_WhenPaymentRequestDtoProvided() {
        PaymentRequestDto paymentRequestDto = PaymentRequestDtoGenerator.generatePaymentRequestDto();

        OrderEventDto orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);

        assertEquals(paymentRequestDto.getOrderId(), orderEventDto.getOrderId());
        assertEquals(paymentRequestDto.getUserId(), orderEventDto.getUserId());
        assertEquals(paymentRequestDto.getPaymentAmount(), orderEventDto.getPaymentAmount());
    }
}
