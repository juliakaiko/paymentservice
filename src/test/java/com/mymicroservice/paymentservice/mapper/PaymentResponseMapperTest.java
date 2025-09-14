package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.PaymentEventDto;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PaymentResponseMapperTest {

    @Test
    public void testPaymentEntityToPaymentEventDto_whenOk_thenMapFieldsCorrectly() {
        List<PaymentEntity> paymentEntities = PaymentEntitiesGenerator.generatePaymentEntities();
        PaymentEntity testPayment = paymentEntities.get(0);
        PaymentEventDto paymentEventDto = PaymentResponseMapper.INSTANCE.toDto(testPayment);

        assertEquals(testPayment.getId(), paymentEventDto.getId());
        assertEquals(testPayment.getOrderId(), paymentEventDto.getOrderId());
        assertEquals(testPayment.getUserId(), paymentEventDto.getUserId());
        assertEquals(testPayment.getStatus().toString(), paymentEventDto.getStatus());
        assertEquals(testPayment.getTimestamp().toString(), paymentEventDto.getTimestamp());
        assertEquals(testPayment.getPaymentAmount(), paymentEventDto.getPaymentAmount());
    }

    @Test
    public void testPaymentEventDtoToPaymentEntity_whenOk_thenMapFieldsCorrectly() {
        List<PaymentEntity> paymentEntities = PaymentEntitiesGenerator.generatePaymentEntities();
        PaymentEntity testPayment = paymentEntities.get(0);
        PaymentEventDto paymentEventDto = PaymentResponseMapper.INSTANCE.toDto(testPayment);
        testPayment = PaymentResponseMapper.INSTANCE.toEntity(paymentEventDto);

        assertEquals(testPayment.getId(), paymentEventDto.getId());
        assertEquals(testPayment.getOrderId(), paymentEventDto.getOrderId());
        assertEquals(testPayment.getUserId(), paymentEventDto.getUserId());
        assertEquals(testPayment.getStatus().toString(), paymentEventDto.getStatus());
        assertEquals(testPayment.getTimestamp().toString(), paymentEventDto.getTimestamp());
        assertEquals(testPayment.getPaymentAmount(), paymentEventDto.getPaymentAmount());
    }
}
