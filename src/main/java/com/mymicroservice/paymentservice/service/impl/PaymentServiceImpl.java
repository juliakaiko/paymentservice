package com.mymicroservice.paymentservice.service.impl;

import com.mymicroservice.paymentservice.exception.PaymentNotFoundException;
import com.mymicroservice.paymentservice.kafka.EventEnvelope;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.mapper.OrderEventMapper;
import com.mymicroservice.paymentservice.mapper.PaymentResponseMapper;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.service.PaymentService;
import com.mymicroservice.paymentservice.webclient.RandomNumberClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Profile({"dev", "default"})
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentEventProducer paymentEventProducer;
    private final PaymentRepository paymentRepository;
    private final RandomNumberClient randomNumberClient;

    @Override
    public PaymentEventDto createPayment(EventEnvelope<OrderEventDto> eventEnvelope) {
        return paymentRepository.findFirstByOrderId(eventEnvelope.payload().getOrderId())
                .map(existing -> {
                    log.info("Payment already exists for orderId={}, returning existing paymentId={}",
                            eventEnvelope.payload().getOrderId(), existing.getId());
                    return PaymentResponseMapper.INSTANCE.toDto(existing);
                })
                .orElseGet(() -> saveNewPayment(eventEnvelope));
    }

    private PaymentEventDto saveNewPayment (EventEnvelope<OrderEventDto> eventEnvelope) {
        Payment entity = OrderEventMapper.INSTANCE.toEntity(eventEnvelope.payload());
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? PaymentStatus.PAID : PaymentStatus.FAILED);
        log.info("createPayment(): {}", entity);

        try {
            PaymentEventDto responseDto = PaymentResponseMapper.INSTANCE.toDto(paymentRepository.save(entity));
            EventEnvelope<PaymentEventDto> paymentEventDto = new EventEnvelope<>(
                    responseDto,
                    eventEnvelope.traceId(),
                    eventEnvelope.sourceService(),
                    eventEnvelope.idempotenceId()
            );
            paymentEventProducer.sendCreatePayment(paymentEventDto);
            return responseDto;
        } catch (DuplicateKeyException ex) {
            Payment existing = paymentRepository.findFirstByOrderId(eventEnvelope.payload().getOrderId())
                    .orElseThrow(() -> ex);
            log.info("Duplicate payment creation attempt detected for orderId={}, returning existing paymentId={}",
                    eventEnvelope.payload().getOrderId(), existing.getId());
            return PaymentResponseMapper.INSTANCE.toDto(existing);
        }
    }

    @Override
    public PaymentEventDto getPaymentById(String id) {
        Payment entity = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment wasn't found with id " + id));
        log.info("getPaymentById(): {}", id);
        return PaymentResponseMapper.INSTANCE.toDto(entity);
    }

    @Override
    public PaymentEventDto updatePayment(String id, OrderEventDto dtoDetails) {
        Payment entity = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment wasn't found with id " + id));
        entity.setOrderId(dtoDetails.getOrderId());
        entity.setUserId(dtoDetails.getUserId());
        entity.setPaymentAmount(dtoDetails.getPaymentAmount());
        int random = randomNumberClient.generateRandNum();
        entity.setStatus(random % 2 == 0 ? PaymentStatus.PAID : PaymentStatus.FAILED);
        log.info("updatePayment(): {}", entity);

        return PaymentResponseMapper.INSTANCE.toDto(paymentRepository.save(entity));
    }

    @Override
    public PaymentEventDto deletePaymentById(String id) {
        Payment entity = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment wasn't found with id " + id));
        paymentRepository.deleteById(id);
        log.info("deletePaymentById(): {}", id);
        return PaymentResponseMapper.INSTANCE.toDto(entity);
    }

    @Override
    public List<PaymentEventDto> getPaymentsByOrderId(String orderId) {
        log.info("getPaymentsByOrderId(): {}", orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    @Override
    public List<PaymentEventDto> getPaymentsByUserId(String userId) {
        log.info("getPaymentsByUserId(): {}", userId);
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    @Override
    public List<PaymentEventDto> getPaymentsByStatuses(List<String> statuses) {
        log.info("getPaymentsByStatuses(): {}", statuses);
        List<Payment> payments = paymentRepository.findByStatusIn(statuses);
        return payments.stream().map(PaymentResponseMapper.INSTANCE::toDto).toList();
    }

    @Override
    public BigDecimal getTotalSumForPeriod(LocalDateTime start, LocalDateTime end) {
        log.info("getTotalSumForPeriod(): {} - {}", start, end);
        List<Payment> payments = paymentRepository.findByTimestampBetween(start, end);
        payments.forEach(payment ->
                log.info("Payment: id={}, timestamp={}, amount={}, status={}",
                        payment.getId(), payment.getTimestamp(), payment.getPaymentAmount(), payment.getStatus())
        );
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getPaymentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
