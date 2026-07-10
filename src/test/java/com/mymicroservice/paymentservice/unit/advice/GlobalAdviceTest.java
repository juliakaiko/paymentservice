package com.mymicroservice.paymentservice.unit.advice;

import com.mymicroservice.paymentservice.advice.GlobalAdvice;
import com.mymicroservice.paymentservice.exception.InboxEventNotFoundException;
import com.mymicroservice.paymentservice.exception.PaymentNotFoundException;
import com.mymicroservice.paymentservice.util.ErrorItem;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalAdviceTest {

    private final GlobalAdvice globalAdvice = new GlobalAdvice();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest_WhenValidationFails() {
        setRequestContext("/api/payments");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "orderId", "must not be null"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorItem> response = globalAdvice.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleValidationException_ShouldReturnBadRequest_WhenConstraintViolated() {
        setRequestContext("/api/payments/statuses");
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        ResponseEntity<ErrorItem> response = globalAdvice.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleDataIntegrityViolationException_ShouldReturnBadRequest_WhenConstraintViolated() {
        setRequestContext("/api/payments");

        ResponseEntity<ErrorItem> response = globalAdvice.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleHttpMessageNotReadableException_ShouldReturnBadRequest_WhenBodyInvalid() {
        setRequestContext("/api/payments");

        ResponseEntity<ErrorItem> response = globalAdvice.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("invalid json"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatusCode());
    }

    @Test
    void handlePaymentNotFoundException_ShouldReturnNotFound_WhenPaymentMissing() {
        setRequestContext("/api/payments/1");

        ResponseEntity<ErrorItem> response = globalAdvice.handlePaymentNotFoundException(
                new PaymentNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    @Test
    void handleOutboxEventNotFoundException_ShouldReturnNotFound_WhenInboxEventMissing() {
        setRequestContext("/api/inbox/1");

        ResponseEntity<ErrorItem> response = globalAdvice.handleOutboxEventNotFoundException(
                new InboxEventNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatusCode());
    }

    private void setRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
