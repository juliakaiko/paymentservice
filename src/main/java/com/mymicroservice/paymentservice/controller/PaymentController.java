package com.mymicroservice.paymentservice.controller;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.mapper.PaymentRequestMapper;
import com.mymicroservice.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/payments")
@RequiredArgsConstructor
@Tag(name="UserController")
@Slf4j
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/")
    public ResponseEntity<PaymentEventDto> createPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        log.info("Request to add a new Payment record: {}", paymentRequestDto);
        OrderEventDto orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);

        PaymentEventDto savedPayment = paymentService.createPayment(orderEventDto);

        return ObjectUtils.isEmpty(savedPayment)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(savedPayment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById (@PathVariable("id") String id) {
        log.info("Request to find the Payment record by id: {}", id);

        PaymentEventDto paymentDto = paymentService.getPaymentById(id);

        return ObjectUtils.isEmpty(paymentDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(paymentDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePayment (@PathVariable("id") String id,
                                            @RequestBody @Valid PaymentRequestDto dtoDetails){
        log.info("Request to update the Payment record: {}", dtoDetails);

        OrderEventDto orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(dtoDetails);

        PaymentEventDto updatedPayment = paymentService.updatePayment(id, orderEventDto);

        return ObjectUtils.isEmpty(updatedPayment)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedPayment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment (@PathVariable("id") String id){
        log.info("Request to delete the Payment by id: {}", id);

        PaymentEventDto deletedPaymentDto = paymentService.deletePaymentById(id);

        return ObjectUtils.isEmpty(deletedPaymentDto)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(deletedPaymentDto);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentEventDto>> getPaymentsbyOrder(@PathVariable String orderId) {
        log.info("Request to get payment records by orderId: {}", orderId);
        List<PaymentEventDto> paymentDtos = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(paymentDtos);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentEventDto>> getPaymentsbyUser(@PathVariable String userId) {
        log.info("Request to get payment records by userId: {}", userId);
        List<PaymentEventDto> paymentDtos = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(paymentDtos);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<PaymentEventDto>> getPaymentsbyStatuses(@RequestParam List<String> statuses) {
        log.info("Request to get payment records by statuses: {}", statuses);
        List<PaymentEventDto> paymentDtos = paymentService.getPaymentsByStatuses(statuses);
        return ResponseEntity.ok(paymentDtos);
    }

    @GetMapping("/sum")
    public ResponseEntity<BigDecimal> getTotalSumForPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("Request to get the sum of payments for the period: {} - {}", start, end);
        return ResponseEntity.ok(paymentService.getTotalSumForPeriod(start, end));
    }
}
