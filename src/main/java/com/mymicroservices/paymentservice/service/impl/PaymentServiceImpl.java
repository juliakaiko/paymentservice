package com.mymicroservices.paymentservice.service.impl;

import com.mymicroservices.paymentservice.dto.PaymentRequestDto;
import com.mymicroservices.paymentservice.dto.PaymentResponseDto;
import com.mymicroservices.paymentservice.mapper.PaymentRequestMapper;
import com.mymicroservices.paymentservice.mapper.PaymentResponseMapper;
import com.mymicroservices.paymentservice.model.PaymentEntity;
import com.mymicroservices.paymentservice.model.Status;
import com.mymicroservices.paymentservice.repository.PaymentRepository;
import com.mymicroservices.paymentservice.service.PaymentService;
import com.mymicroservices.paymentservice.webclient.RandomNumberClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RandomNumberClient randomNumberClient;

    public PaymentResponseDto createPayment(PaymentRequestDto dto) {
        PaymentEntity entity = PaymentRequestMapper.INSTANSE.toEntity(dto);
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? Status.SUCCESS : Status.FAILED);
        //entity.setTimestamp(LocalDateTime.now());
        log.info("createPayment(): {}", entity);

        return PaymentResponseMapper.INSTANSE.toDto(paymentRepository.save(entity));
    }

    public PaymentResponseDto getPaymentById(String id) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        log.info("getPaymentById(): {}", id);
        return PaymentResponseMapper.INSTANSE.toDto(entity);
    }

    public PaymentResponseDto updatePayment(String id, PaymentRequestDto dtoDetails) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        entity.setOrderId(dtoDetails.getOrderId());
        entity.setUserId(dtoDetails.getUserId());
        entity.setPaymentAmount(dtoDetails.getPaymentAmount());
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? Status.SUCCESS : Status.FAILED);
        log.info("updatePayment(): {}", entity);

        return PaymentResponseMapper.INSTANSE.toDto(paymentRepository.save(entity));
    }

    public PaymentResponseDto deletePaymentById(String id) {
        PaymentEntity entity = paymentRepository.findById(id).get();
        paymentRepository.deleteById(id);
        log.info("deletePaymentById(): {}", id);
        return PaymentResponseMapper.INSTANSE.toDto(entity);
    }

    public List<PaymentResponseDto> getPaymentsByOrderId(String orderId) {
        log.info("getPaymentsByOrderId(): {}", orderId);
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream().map(PaymentResponseMapper.INSTANSE::toDto).toList();
    }

    public List<PaymentResponseDto> getPaymentsByUserId(String userId) {
        log.info("getPaymentsByUserId(): {}", userId);
        List<PaymentEntity> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(PaymentResponseMapper.INSTANSE::toDto).toList();
    }

    public List<PaymentResponseDto> getPaymentsByStatuses(List<String> statuses) {
        log.info("getPaymentsByStatuses(): {}", statuses);
        List<PaymentEntity> payments = paymentRepository.findByStatusIn(statuses);
        return payments.stream().map(PaymentResponseMapper.INSTANSE::toDto).toList();
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
