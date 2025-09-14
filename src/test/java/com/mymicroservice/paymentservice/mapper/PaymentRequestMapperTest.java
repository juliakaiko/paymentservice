package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.util.PaymentRequestDtoGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import static org.junit.Assert.assertEquals;

public class PaymentRequestMapperTest {

    @Test
    public void testPaymentRequestDtoToOrderEventDto_whenOk_thenMapFieldsCorrectly() {
        PaymentRequestDto paymentRequestDto = PaymentRequestDtoGenerator.generatePaymentRequestDto();
        OrderEventDto orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);

        assertEquals(orderEventDto.getOrderId(), paymentRequestDto.getOrderId());
        assertEquals(orderEventDto.getUserId(), paymentRequestDto.getUserId());
        assertEquals(orderEventDto.getPaymentAmount(), paymentRequestDto.getPaymentAmount());
    }
}
