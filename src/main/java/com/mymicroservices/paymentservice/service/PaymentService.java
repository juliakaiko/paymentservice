package com.mymicroservices.paymentservice.service;

import com.mymicroservices.paymentservice.dto.PaymentRequestDto;
import com.mymicroservices.paymentservice.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto dto);
    PaymentResponseDto getPaymentById(String id);
    PaymentResponseDto updatePayment(String id, PaymentRequestDto dtoDetails);
    PaymentResponseDto deletePaymentById(String id);
    List<PaymentResponseDto> getPaymentsByOrderId(String orderId);
    List<PaymentResponseDto> getPaymentsByUserId(String userId);
    List<PaymentResponseDto> getPaymentsByStatuses(List<String> statuses);
    BigDecimal getTotalSumForPeriod(LocalDateTime start, LocalDateTime end);

}
