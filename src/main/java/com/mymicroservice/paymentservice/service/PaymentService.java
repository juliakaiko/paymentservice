package com.mymicroservice.paymentservice.service;

import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentEventDto createPayment(OrderEventDto dto);
    PaymentEventDto getPaymentById(String id);
    PaymentEventDto updatePayment(String id, OrderEventDto dtoDetails);
    PaymentEventDto deletePaymentById(String id);
    List<PaymentEventDto> getPaymentsByOrderId(String orderId);
    List<PaymentEventDto> getPaymentsByUserId(String userId);
    List<PaymentEventDto> getPaymentsByStatuses(List<String> statuses);
    BigDecimal getTotalSumForPeriod(LocalDateTime start, LocalDateTime end);

}
