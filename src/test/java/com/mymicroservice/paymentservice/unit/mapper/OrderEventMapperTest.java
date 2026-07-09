package com.mymicroservice.paymentservice.unit.mapper;

import com.mymicroservice.paymentservice.mapper.OrderEventMapper;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderEventMapperTest {

    @Test
    void toEntity_ShouldMapFieldsCorrectly_WhenOrderEventDtoProvided() {
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto();

        Payment payment = OrderEventMapper.INSTANCE.toEntity(orderEventDto);

        assertEquals(orderEventDto.getOrderId(), payment.getOrderId());
        assertEquals(orderEventDto.getUserId(), payment.getUserId());
        assertEquals(orderEventDto.getPaymentAmount(), payment.getPaymentAmount());
        assertNotNull(payment.getTimestamp());
    }
}
