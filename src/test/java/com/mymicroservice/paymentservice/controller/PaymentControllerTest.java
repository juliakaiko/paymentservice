package com.mymicroservice.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.mapper.PaymentRequestMapper;
import com.mymicroservice.paymentservice.mapper.PaymentResponseMapper;
import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.service.PaymentService;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mymicroservices.common.events.OrderEventDto;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PaymentController.class)
@Slf4j
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("testPaymentService")
    private PaymentService paymentService; // from TestConfig

    @TestConfiguration
    static class TestConfig {
        @Bean("testPaymentService")
        PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }
    }

    private final static String PAYMENT_ID = "test-payment-1";
    private List<PaymentEntity> expectedPayments;
    private PaymentEventDto paymentEventDto;
    private PaymentRequestDto paymentRequestDto;
    private OrderEventDto orderEventDto;

    @BeforeEach
    void init() {
        expectedPayments = PaymentEntitiesGenerator.generatePaymentEntities();
        paymentEventDto = PaymentResponseMapper.INSTANCE.toDto(expectedPayments.get(0));
        paymentRequestDto = PaymentRequestDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.valueOf(100000, 2))
                .build();
        orderEventDto = PaymentRequestMapper.INSTANCE.toOrderEventDto(paymentRequestDto);
    }

    @Test
    public void createPayment_ShouldReturnCreatedPaymentEventDto() throws Exception {
        log.info("▶ Running test: createPayment_ShouldReturnCreatedPaymentEventDto(), OrdertEvent={}", orderEventDto);
        when(paymentService.createPayment(any(OrderEventDto.class))).thenReturn(paymentEventDto);

        mockMvc.perform(post("/api/payments/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentEventDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID));

        verify(paymentService).createPayment(any(OrderEventDto.class));
    }

    @Test
    public void getPaymentById_ShouldReturnPaymentEventDto() throws Exception {
        log.info("▶ Running test: getPaymentById_ShouldReturnPaymentEventDto(), PAYMENT_ID={}", PAYMENT_ID);
        when(paymentService.getPaymentById(PAYMENT_ID)).thenReturn(paymentEventDto);

        mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID));

        verify(paymentService, atLeastOnce()).getPaymentById(PAYMENT_ID);
    }

    @Test
    public void getPaymentById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: getPaymentById_ShouldReturnNotFound(), PAYMENT_ID={}", PAYMENT_ID);
        when(paymentService.getPaymentById(PAYMENT_ID)).thenReturn(null);

        mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isNotFound());

        verify(paymentService, atLeastOnce()).getPaymentById(PAYMENT_ID);
    }

    @Test
    public void updatePayment_ShouldReturnUpdatedPaymentEventDto() throws Exception {
        when(paymentService.updatePayment(eq(PAYMENT_ID), any(OrderEventDto.class))).thenReturn(paymentEventDto);

        mockMvc.perform(put("/api/payments/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID));

        verify(paymentService, atLeastOnce()).updatePayment(eq(PAYMENT_ID), any(OrderEventDto.class));
    }

    @Test
    public void updatePayment_ShouldReturnNotFound() throws Exception {
        when(paymentService.updatePayment(eq(PAYMENT_ID), any(OrderEventDto.class))).thenReturn(null);

        mockMvc.perform(put("/api/payments/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequestDto)))
                .andExpect(status().isNotFound());

        verify(paymentService).updatePayment(eq(PAYMENT_ID), any(OrderEventDto.class));
    }

    @Test
    public void deletePayment_ShouldReturnDeletedPaymentEventDto() throws Exception {
        when(paymentService.deletePaymentById(PAYMENT_ID)).thenReturn(paymentEventDto);

        mockMvc.perform(delete("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID));

        verify(paymentService, atLeastOnce()).deletePaymentById(PAYMENT_ID);
    }

    @Test
    public void deletePayment_ShouldReturnNotFound() throws Exception {
        when(paymentService.deletePaymentById(PAYMENT_ID)).thenReturn(null);

        mockMvc.perform(delete("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isNotFound());

        verify(paymentService).deletePaymentById(PAYMENT_ID);
    }

    @Test
    public void getPaymentsByOrder_ShouldReturnList() throws Exception {
        when(paymentService.getPaymentsByOrderId("1")).thenReturn(List.of(paymentEventDto));

        mockMvc.perform(get("/api/payments/order/{orderId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID));

        verify(paymentService).getPaymentsByOrderId("1");
    }

    @Test
    public void getPaymentsByUser_ShouldReturnList() throws Exception {
        when(paymentService.getPaymentsByUserId("1")).thenReturn(List.of(paymentEventDto));

        mockMvc.perform(get("/api/payments/user/{userId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID));

        verify(paymentService).getPaymentsByUserId("1");
    }

    @Test
    public void getPaymentsByStatuses_ShouldReturnList() throws Exception {
        List<String> statuses = Arrays.asList("PAID", "PENDING");
        when(paymentService.getPaymentsByStatuses(statuses)).thenReturn(List.of(paymentEventDto));

        mockMvc.perform(get("/api/payments/statuses")
                        .param("statuses", "PAID", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID));

        verify(paymentService).getPaymentsByStatuses(statuses);
    }

    @Test
    public void getTotalSumForPeriod_ShouldReturnSum() throws Exception {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        BigDecimal expectedSum = BigDecimal.valueOf(12345);

        when(paymentService.getTotalSumForPeriod(start, end)).thenReturn(expectedSum);

        mockMvc.perform(get("/api/payments/sum")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expectedSum.intValue())); // jsonPath for  BigDecimal → intValue

        verify(paymentService).getTotalSumForPeriod(start, end);
    }

}
