package com.mymicroservice.paymentservice.unit.util;

import com.mymicroservice.paymentservice.util.ErrorItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ErrorItemTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void generateMessage_ShouldPopulateFields_WhenExceptionIsProvided() {
        setRequestContext("/api/payments");

        ErrorItem error = ErrorItem.generateMessage(new RuntimeException("boom"), HttpStatus.BAD_REQUEST);

        assertEquals("boom", error.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getStatusCode());
        assertEquals("http://localhost/api/payments", error.getUrl());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void fromMethodArgumentNotValid_ShouldCollectFieldErrors_WhenValidationFails() {
        setRequestContext("/api/payments");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "orderId", "must not be null"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ErrorItem error = ErrorItem.fromMethodArgumentNotValid(exception, HttpStatus.BAD_REQUEST);

        assertEquals("Validation failed", error.getMessage());
        assertEquals("must not be null", error.getFieldErrors().get("orderId"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getStatusCode());
    }

    @Test
    void formatDate_ShouldReturnFormattedTimestamp_WhenCalled() {
        String formatted = ErrorItem.formatDate();

        assertEquals(16, formatted.length());
    }

    @Test
    void fromMethodArgumentNotValid_ShouldMergeDuplicateFieldErrors_WhenSameFieldHasMultipleErrors() {
        setRequestContext("/api/payments");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "orderId", "first"));
        bindingResult.addError(new FieldError("dto", "orderId", "second"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ErrorItem error = ErrorItem.fromMethodArgumentNotValid(exception, HttpStatus.BAD_REQUEST);

        assertEquals("first; second", error.getFieldErrors().get("orderId"));
    }

    private void setRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
