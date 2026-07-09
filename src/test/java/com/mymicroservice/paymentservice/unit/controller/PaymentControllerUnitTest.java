package com.mymicroservice.paymentservice.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.paymentservice.controller.PaymentController;
import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import com.mymicroservice.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymicroservices.common.events.PaymentEventDto;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.mymicroservice.paymentservice.util.PaymentEventDtoGenerator.generatePaymentEventDto;
import static com.mymicroservice.paymentservice.util.PaymentEventDtoGenerator.generateSecondPaymentEventDto;
import static com.mymicroservice.paymentservice.util.PaymentRequestDtoGenerator.generatePaymentRequestDto;
import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.FAILED_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.NON_EXISTENT_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAID_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAYMENT_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_PAYMENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PaymentControllerUnitTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaymentRequestDto paymentRequestDto;
    private PaymentEventDto paymentEventDto;
    private PaymentEventDto secondPaymentEventDto;
    private List<PaymentEventDto> paymentEventDtoList;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(paymentController)
                .build();
        objectMapper = new ObjectMapper();
        paymentRequestDto = generatePaymentRequestDto();
        paymentEventDto = generatePaymentEventDto();
        secondPaymentEventDto = generateSecondPaymentEventDto();
        paymentEventDtoList = Arrays.asList(paymentEventDto, secondPaymentEventDto);
    }

    @Test
    void getPaymentById_ShouldReturnPaymentEventDto() throws Exception {
        log.info("▶ Test: getPaymentById_ShouldReturnPaymentEventDto");

        when(paymentService.getPaymentById(PAYMENT_ID)).thenReturn(paymentEventDto);

        mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                .andExpect(jsonPath("$.orderId").value(ENTITY_ID))
                .andExpect(jsonPath("$.userId").value(ENTITY_ID))
                .andExpect(jsonPath("$.status").value(PAID_STATUS));
    }

    @Test
    void getPaymentById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Test: getPaymentById_ShouldReturnNotFound");

        when(paymentService.getPaymentById(NON_EXISTENT_ID)).thenReturn(null);

        mockMvc.perform(get("/api/payments/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePayment_ShouldReturnUpdatedPayment() throws Exception {
        log.info("▶ Test: updatePayment_ShouldReturnUpdatedPayment");

        when(paymentService.updatePayment(eq(PAYMENT_ID), any()))
                .thenReturn(paymentEventDto);

        mockMvc.perform(put("/api/payments/{id}", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                .andExpect(jsonPath("$.orderId").value(ENTITY_ID));
    }

    @Test
    void updatePayment_ShouldReturnNotFound_WhenPaymentDoesNotExist() throws Exception {
        log.info("▶ Test: updatePayment_ShouldReturnNotFound_WhenPaymentDoesNotExist");

        when(paymentService.updatePayment(eq(NON_EXISTENT_ID), any()))
                .thenReturn(null);

        mockMvc.perform(put("/api/payments/{id}", NON_EXISTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePayment_ShouldReturnDeletedPayment() throws Exception {
        log.info("▶ Test: deletePayment_ShouldReturnDeletedPayment");

        when(paymentService.deletePaymentById(PAYMENT_ID)).thenReturn(paymentEventDto);

        mockMvc.perform(delete("/api/payments/{id}", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID));
    }

    @Test
    void deletePayment_ShouldReturnNotFound_WhenPaymentDoesNotExist() throws Exception {
        log.info("▶ Test: deletePayment_ShouldReturnNotFound_WhenPaymentDoesNotExist");

        when(paymentService.deletePaymentById(NON_EXISTENT_ID)).thenReturn(null);

        mockMvc.perform(delete("/api/payments/{id}", NON_EXISTENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByOrder_ShouldReturnListOfPayments() throws Exception {
        log.info("▶ Test: getPaymentsByOrder_ShouldReturnListOfPayments");

        when(paymentService.getPaymentsByOrderId(ENTITY_ID)).thenReturn(paymentEventDtoList);

        mockMvc.perform(get("/api/payments/order/{orderId}", ENTITY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID))
                .andExpect(jsonPath("$[0].orderId").value(ENTITY_ID))
                .andExpect(jsonPath("$[1].id").value(SECOND_PAYMENT_ID))
                .andExpect(jsonPath("$[1].orderId").value(SECOND_ENTITY_ID))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentsByOrder_ShouldReturnEmptyList_WhenNoPaymentsFound() throws Exception {
        log.info("▶ Test: getPaymentsByOrder_ShouldReturnEmptyList_WhenNoPaymentsFound");

        when(paymentService.getPaymentsByOrderId(NON_EXISTENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/order/{orderId}", NON_EXISTENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPaymentsByUser_ShouldReturnListOfPayments() throws Exception {
        log.info("▶ Test: getPaymentsByUser_ShouldReturnListOfPayments");

        when(paymentService.getPaymentsByUserId(ENTITY_ID)).thenReturn(paymentEventDtoList);

        mockMvc.perform(get("/api/payments/user/{userId}", ENTITY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID))
                .andExpect(jsonPath("$[0].userId").value(ENTITY_ID))
                .andExpect(jsonPath("$[1].id").value(SECOND_PAYMENT_ID))
                .andExpect(jsonPath("$[1].userId").value(SECOND_ENTITY_ID))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentsByUser_ShouldReturnEmptyList_WhenNoPaymentsFound() throws Exception {
        log.info("▶ Test: getPaymentsByUser_ShouldReturnEmptyList_WhenNoPaymentsFound");

        when(paymentService.getPaymentsByUserId(NON_EXISTENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/user/{userId}", NON_EXISTENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPaymentsByStatuses_ShouldReturnListOfPayments() throws Exception {
        log.info("▶ Test: getPaymentsByStatuses_ShouldReturnListOfPayments");

        List<String> statuses = Arrays.asList(PAID_STATUS, FAILED_STATUS);
        when(paymentService.getPaymentsByStatuses(statuses)).thenReturn(paymentEventDtoList);

        mockMvc.perform(get("/api/payments/statuses")
                        .param("statuses", PAID_STATUS, FAILED_STATUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PAYMENT_ID))
                .andExpect(jsonPath("$[0].status").value(PAID_STATUS))
                .andExpect(jsonPath("$[1].status").value(FAILED_STATUS))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentsByStatuses_ShouldReturnEmptyList_WhenNoPaymentsFound() throws Exception {
        log.info("▶ Test: getPaymentsByStatuses_ShouldReturnEmptyList_WhenNoPaymentsFound");

        List<String> statuses = Arrays.asList("REFUNDED", "CANCELLED");
        when(paymentService.getPaymentsByStatuses(statuses)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/statuses")
                        .param("statuses", "REFUNDED", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTotalSumForPeriod_ShouldReturnSum() throws Exception {
        log.info("▶ Test: getTotalSumForPeriod_ShouldReturnSum");

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        BigDecimal expectedSum = BigDecimal.valueOf(12345.67);

        when(paymentService.getTotalSumForPeriod(start, end)).thenReturn(expectedSum);

        mockMvc.perform(get("/api/payments/sum")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12345.67));
    }

    @Test
    void getTotalSumForPeriod_ShouldReturnZero_WhenNoPaymentsInPeriod() throws Exception {
        log.info("▶ Test: getTotalSumForPeriod_ShouldReturnZero_WhenNoPaymentsInPeriod");

        LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2020, 12, 31, 23, 59);
        BigDecimal expectedSum = BigDecimal.ZERO;

        when(paymentService.getTotalSumForPeriod(start, end)).thenReturn(expectedSum);

        mockMvc.perform(get("/api/payments/sum")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void getTotalSumForPeriod_WithDateTimeFormat_ShouldHandleISOFormat() throws Exception {
        log.info("▶ Test: getTotalSumForPeriod_WithDateTimeFormat_ShouldHandleISOFormat");

        String start = "2025-01-01T00:00:00";
        String end = "2025-12-31T23:59:59";
        BigDecimal expectedSum = BigDecimal.valueOf(500.00);

        LocalDateTime expectedStart = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expectedEnd = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        when(paymentService.getTotalSumForPeriod(expectedStart, expectedEnd)).thenReturn(expectedSum);

        mockMvc.perform(get("/api/payments/sum")
                        .param("start", start)
                        .param("end", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(500.00));
    }
}