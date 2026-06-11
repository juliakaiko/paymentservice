package com.mymicroservice.paymentservice.unit.mapper;

import com.mymicroservice.paymentservice.mapper.PaymentResponseMapper;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.PaymentEventDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentResponseMapperTest {

    @Test
    void toDto_ShouldMapFieldsCorrectly_WhenPaymentEntityProvided() {
        List<Payment> paymentEntities = PaymentEntitiesGenerator.generatePaymentEntities();
        Payment testPayment = paymentEntities.get(0);

        PaymentEventDto paymentEventDto = PaymentResponseMapper.INSTANCE.toDto(testPayment);

        assertEquals(testPayment.getId(), paymentEventDto.getId());
        assertEquals(testPayment.getOrderId(), paymentEventDto.getOrderId());
        assertEquals(testPayment.getUserId(), paymentEventDto.getUserId());
        assertEquals(testPayment.getStatus().toString(), paymentEventDto.getStatus());
        assertEquals(testPayment.getTimestamp().toString(), paymentEventDto.getTimestamp());
        assertEquals(testPayment.getPaymentAmount(), paymentEventDto.getPaymentAmount());
    }

    @Test
    void toEntity_ShouldMapFieldsCorrectly_WhenPaymentEventDtoProvided() {
        List<Payment> paymentEntities = PaymentEntitiesGenerator.generatePaymentEntities();
        Payment testPayment = paymentEntities.get(0);
        PaymentEventDto paymentEventDto = PaymentResponseMapper.INSTANCE.toDto(testPayment);

        Payment mappedEntity = PaymentResponseMapper.INSTANCE.toEntity(paymentEventDto);

        assertEquals(testPayment.getId(), mappedEntity.getId());
        assertEquals(testPayment.getOrderId(), mappedEntity.getOrderId());
        assertEquals(testPayment.getUserId(), mappedEntity.getUserId());
        assertEquals(testPayment.getStatus().toString(), paymentEventDto.getStatus());
        assertEquals(testPayment.getTimestamp().toString(), paymentEventDto.getTimestamp());
        assertEquals(testPayment.getPaymentAmount(), mappedEntity.getPaymentAmount());
    }
}
