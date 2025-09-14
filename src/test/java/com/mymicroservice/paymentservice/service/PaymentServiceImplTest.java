package com.mymicroservice.paymentservice.service;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.exception.PaymentNotFoundException;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.mapper.PaymentRequestMapper;
import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.impl.PaymentServiceImpl;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import com.mymicroservice.paymentservice.webclient.RandomNumberClient;
import lombok.extern.slf4j.Slf4j;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PaymentServiceImplTest {

    @InjectMocks
    PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    private final static String PAYMENT_ID = "test-payment-1";
    private List<PaymentEntity> expectedPayments;
    private PaymentRequestDto paymentRequestDto;
    private OrderEventDto orderEventDto;

    @BeforeEach
    void init() {
        expectedPayments = PaymentEntitiesGenerator.generatePaymentEntities();
        paymentRequestDto = PaymentRequestDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(100000, 2))
                .build();
        orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);
    }

    @Test
    void createNewPayment_ReturnsPaymentEventDto() {
        when(randomNumberClient.generateRandNum()).thenReturn(42);
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(i -> i.getArgument(0));

        PaymentEventDto result = paymentService.createPayment(orderEventDto);
        log.info("createNewPayment_ReturnsPaymentEventDto(): {}", orderEventDto);

        assertEquals(Status.PAID.toString(), result.getStatus());
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
        verify(paymentEventProducer).sendCreatePayment(any());
    }

    @Test
    void getPaymentById_WhenExists_ReturnsPaymentEventDto() {
        PaymentEntity entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(entity));

        PaymentEventDto result = paymentService.getPaymentById(entity.getId());
        log.info("getPaymentById_WhenExists_ReturnsPaymentEventDto(): PaymentId={}", PAYMENT_ID);

        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getOrderId(), result.getOrderId());
    }

    @Test
    void getPaymentById_WhenNotExists_ThrowsException() {
        when(paymentRepository.findById("missing"))
                .thenReturn(Optional.empty());
        log.info("getPaymentById_WhenNotExists_ThrowsException()");

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.getPaymentById("missing"));
    }

    @Test
    void updatePayment_WhenExists_UpdatesAndReturnsPaymentEventDto() {
        PaymentEntity entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(entity));
        when(randomNumberClient.generateRandNum()).thenReturn(99); // uneven number => FAILED
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        PaymentEventDto result = paymentService.updatePayment(entity.getId(), orderEventDto);
        log.info("getPaymentById_WhenExists_ReturnsPaymentEventDto(): PaymentId={}", entity.getId());

        assertEquals(Status.FAILED.toString(), result.getStatus());
        assertEquals(orderEventDto.getOrderId(), result.getOrderId());
        assertEquals(orderEventDto.getUserId(), result.getUserId());
    }

    @Test
    void updatePayment_WhenNotExists_ThrowsException() {
        when(paymentRepository.findById("missing"))
                .thenReturn(Optional.empty());

        log.info("updatePayment_WhenNotExists_ThrowsException()");
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.updatePayment("missing", orderEventDto));
    }

    @Test
    void deletePaymentById_WhenExists_DeletesAndReturnsPaymentEventDto() {
        PaymentEntity entity = expectedPayments.get(0);
        when(paymentRepository.findById(PAYMENT_ID))
                .thenReturn(Optional.of(entity));

        PaymentEventDto result = paymentService.deletePaymentById(entity.getId());
        log.info("deletePaymentById_WhenExists_DeletesAndReturnsPaymentEventDto(): PaymentId={}", entity.getId());

        assertEquals(entity.getId(), result.getId());
        verify(paymentRepository, times(1)).deleteById(entity.getId());
    }

    @Test
    void deletePaymentById_WhenNotExists_ThrowsException() {
        when(paymentRepository.findById("missing"))
                .thenReturn(Optional.empty());

        log.info("deletePaymentById_WhenNotExists_ThrowsException()");
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.deletePaymentById("missing"));
    }

    @Test
    void getPaymentsByOrderId_ReturnsList() {
        when(paymentRepository.findByOrderId("1"))
                .thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByOrderId("1");
        log.info("getPaymentsByOrderId_ReturnsList()");

        assertEquals(expectedPayments.size(), result.size());
        assertEquals(expectedPayments.get(0).getOrderId(), result.get(0).getOrderId());
    }

    @Test
    void getPaymentsByUserId_ReturnsList() {
        when(paymentRepository.findByUserId("1"))
                .thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByUserId("1");
        log.info("getPaymentsByUserId_ReturnsList()");

        assertEquals(expectedPayments.size(), result.size());
        assertEquals(expectedPayments.get(0).getUserId(), result.get(0).getUserId());
    }

    @Test
    void getPaymentsByStatuses_ReturnsList() {
        when(paymentRepository.findByStatusIn(List.of("PAID", "FAILED")))
                .thenReturn(expectedPayments);

        List<PaymentEventDto> result = paymentService.getPaymentsByStatuses(List.of("PAID", "FAILED"));
        log.info("getPaymentsByStatuses_ReturnsList()");

        assertEquals(expectedPayments.size(), result.size());
        assertTrue(List.of("PAID", "FAILED").contains(result.get(0).getStatus()));
    }

    @Test
    void getTotalSumForPeriod_ReturnsSum() {
        PaymentEntity e1 = expectedPayments.get(0);
        PaymentEntity e2 = expectedPayments.get(1);

        when(paymentRepository.findByTimestampBetween(any(), any()))
                .thenReturn(List.of(e1, e2));
        log.info("getTotalSumForPeriod_ReturnsSum()");

        BigDecimal sum = paymentService.getTotalSumForPeriod(
                LocalDateTime.of(2025, 1, 1, 1, 10, 1).minusDays(1),
                LocalDateTime.of(2025, 2, 2, 2, 20, 2).plusDays(1));

        assertEquals(BigDecimal.valueOf(300000, 2), sum);
    }

}
