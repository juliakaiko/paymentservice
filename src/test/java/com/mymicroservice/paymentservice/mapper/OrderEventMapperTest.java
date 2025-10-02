package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import static org.junit.Assert.assertEquals;

public class OrderEventMapperTest {

    @Test
    public void testOrderEventDtoToEntity_whenOk_thenMapFieldsCorrectly() {
        OrderEventDto orderEventDto = OrderEventDtoGenerator.generateOrderEventDto();
        PaymentEntity paymentEntity = OrderEventMapper.INSTANCE.toEntity(orderEventDto);

        assertEquals(orderEventDto.getOrderId(), paymentEntity.getOrderId());
        assertEquals(orderEventDto.getUserId(), paymentEntity.getUserId());
        assertEquals(java.time.LocalDateTime.now().withNano(0), paymentEntity.getTimestamp());
        assertEquals(orderEventDto.getPaymentAmount(), paymentEntity.getPaymentAmount());
    }
}
