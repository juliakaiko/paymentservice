package com.mymicroservice.paymentservice.service.impl;

import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import com.mymicroservice.paymentservice.mapper.OrderEventMapper;
import com.mymicroservice.paymentservice.mapper.PaymentResponseMapper;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.PaymentService;
import com.mymicroservice.paymentservice.webclient.RandomNumberClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentEventDto createPayment(OrderEventDto dto) {
        PaymentEntity entity = OrderEventMapper.INSTANCE.toEntity(dto);
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? Status.PAID : Status.FAILED);
        log.info("createPayment(): {}", entity);
        PaymentEventDto responseDto = PaymentResponseMapper.INSTANCE.toDto(paymentRepository.save(entity));
        paymentEventProducer.sendCreatePayment(responseDto);

        return responseDto;
    }

    public PaymentEventDto getPaymentById(String id) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        log.info("getPaymentById(): {}", id);
        return PaymentResponseMapper.INSTANCE.toDto(entity);
    }

    public PaymentEventDto updatePayment(String id, OrderEventDto dtoDetails) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        entity.setOrderId(dtoDetails.getOrderId());
        entity.setUserId(dtoDetails.getUserId());
        entity.setPaymentAmount(dtoDetails.getPaymentAmount());
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? Status.PAID : Status.FAILED);
        log.info("updatePayment(): {}", entity);

        return PaymentResponseMapper.INSTANCE.toDto(paymentRepository.save(entity));
    }

    public PaymentEventDto deletePaymentById(String id) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        paymentRepository.deleteById(id);
        log.info("deletePaymentById(): {}", id);
        return PaymentResponseMapper.INSTANCE.toDto(entity);
    }

    public List<PaymentEventDto> getPaymentsByOrderId(String orderId) {
        log.info("getPaymentsByOrderId(): {}", orderId);
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    public List<PaymentEventDto> getPaymentsByUserId(String userId) {
        log.info("getPaymentsByUserId(): {}", userId);
        List<PaymentEntity> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    public List<PaymentEventDto> getPaymentsByStatuses(List<String> statuses) {
        log.info("getPaymentsByStatuses(): {}", statuses);
        List<PaymentEntity> payments = paymentRepository.findByStatusIn(statuses);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    public BigDecimal getTotalSumForPeriod(LocalDateTime start, LocalDateTime end) {
        log.info("getTotalSumForPeriod(): {} - {}", start, end);
        List<PaymentEntity> payments = paymentRepository.findByTimestampBetween(start, end);
        payments.forEach(payment ->
                log.info("Payment: id={}, timestamp={}, amount={}, status={}",
                        payment.getId(), payment.getTimestamp(), payment.getPaymentAmount(), payment.getStatus())
        );
        return payments.stream()
                .map(PaymentEntity::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
