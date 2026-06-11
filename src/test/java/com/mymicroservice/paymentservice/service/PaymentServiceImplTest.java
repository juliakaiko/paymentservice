package com.mymicroservice.paymentservice.service;

import com.mymicroservice.paymentservice.exception.PaymentNotFoundException;
import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.impl.PaymentServiceImpl;
import com.mymicroservice.paymentservice.util.EventEnvelopeGenerator;
import com.mymicroservice.paymentservice.util.OrderEventDtoGenerator;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import com.mymicroservice.paymentservice.webclient.RandomNumberClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mymicroservice.paymentservice.util.TestConstants.PAYMENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    private List<Payment> expectedPayments;
    private EventEnvelope<OrderEventDto> eventEnvelope;
    private OrderEventDto orderEventDto;

    @BeforeEach
    void init() {
        expectedPayments = PaymentEntitiesGenerator.generatePaymentEntities();
        orderEventDto = OrderEventDtoGenerator.generateOrderEventDto();
        eventEnvelope = EventEnvelopeGenerator.generateOrderEventEnvelope();
    }

    @Test
    void createPayment_ShouldReturnPaymentEventDto_WhenPaymentDoesNotExist() {
        when(paymentRepository.findFirstByOrderId("1")).thenReturn(Optional.empty());
        when(randomNumberClient.generateRandNum()).thenReturn(42);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentEventDto result = paymentService.createPayment(eventEnvelope);

        assertEquals(PaymentStatus.PAID.toString(), result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentEventProducer).sendCreatePayment(any());
    }

    @Test
    void createPayment_ShouldReturnExistingPayment_WhenOrderAlreadyHasPayment() {
        Payment existing = expectedPayments.get(0);
        when(paymentRepository.findFirstByOrderId("1")).thenReturn(Optional.of(existing));

        PaymentEventDto result = paymentService.createPayment(eventEnvelope);

        assertEquals(existing.getId(), result.getId());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentEventProducer, never()).sendCreatePayment(any());
    }

    @Test
    void getPaymentById_ShouldReturnPaymentEventDto_WhenPaymentExists() {
        Payment entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(entity));

        PaymentEventDto result = paymentService.getPaymentById(entity.getId());

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getOrderId(), result.getOrderId());
    }

    @Test
    void getPaymentById_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getPaymentById("missing"));
    }

    @Test
    void updatePayment_ShouldReturnUpdatedPaymentEventDto_WhenPaymentExists() {
        Payment entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(entity));
        when(randomNumberClient.generateRandNum()).thenReturn(99);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentEventDto result = paymentService.updatePayment(entity.getId(), orderEventDto);

        assertEquals(PaymentStatus.FAILED.toString(), result.getStatus());
        assertEquals(orderEventDto.getOrderId(), result.getOrderId());
        assertEquals(orderEventDto.getUserId(), result.getUserId());
    }

    @Test
    void updatePayment_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.updatePayment("missing", orderEventDto));
    }

    @Test
    void deletePaymentById_ShouldReturnPaymentEventDto_WhenPaymentExists() {
        Payment entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(entity));

        PaymentEventDto result = paymentService.deletePaymentById(entity.getId());

        assertEquals(entity.getId(), result.getId());
        verify(paymentRepository).deleteById(entity.getId());
    }

    @Test
    void deletePaymentById_ShouldThrowException_WhenPaymentNotFound() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.deletePaymentById("missing"));
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnList_WhenPaymentsExist() {
        when(paymentRepository.findByOrderId("1")).thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByOrderId("1");

        assertEquals(expectedPayments.size(), result.size());
        assertEquals(expectedPayments.get(0).getOrderId(), result.get(0).getOrderId());
    }

    @Test
    void getPaymentsByUserId_ShouldReturnList_WhenPaymentsExist() {
        when(paymentRepository.findByUserId("1")).thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByUserId("1");

        assertEquals(expectedPayments.size(), result.size());
        assertEquals(expectedPayments.get(0).getUserId(), result.get(0).getUserId());
    }

    @Test
    void getPaymentsByStatuses_ShouldReturnList_WhenPaymentsExist() {
        when(paymentRepository.findByStatusIn(List.of("PAID", "FAILED")))
                .thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByStatuses(List.of("PAID", "FAILED"));

        assertEquals(expectedPayments.size(), result.size());
        assertTrue(List.of("PAID", "FAILED").contains(result.get(0).getStatus()));
    }

    @Test
    void getTotalSumForPeriod_ShouldReturnSum_WhenPaymentsExistInPeriod() {
        Payment e1 = expectedPayments.get(0);

        when(paymentRepository.findByTimestampBetween(any(), any()))
                .thenReturn(List.of(e1));

        BigDecimal sum = paymentService.getTotalSumForPeriod(
                LocalDateTime.of(2025, 1, 1, 1, 10, 1).minusDays(1),
                LocalDateTime.of(2025, 2, 2, 2, 20, 2).plusDays(1));

        assertEquals(BigDecimal.valueOf(1000.00), sum);
    }
}
