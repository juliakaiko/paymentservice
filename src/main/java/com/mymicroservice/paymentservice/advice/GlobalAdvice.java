package com.mymicroservice.paymentservice.advice;

import com.mymicroservice.paymentservice.exception.PaymentNotFoundException;
import com.mymicroservice.paymentservice.util.ErrorItem;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalAdvice {

    /**
     * Handles validation exceptions for DTO fields when data fails validation annotations
     * such as @Valid, @NotNull, @Size, @Pattern and others.
     *
     * @param e MethodArgumentNotValidException containing validation error information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - List of error messages
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorItem> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorItem error = ErrorItem.fromMethodArgumentNotValid(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles validation exceptions for controller method parameters,
     * such as @NotEmpty, @NotBlank and others.
     *
     * @param e ConstraintViolationException containing validation error information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - Error message
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ErrorItem> handleValidationException(ConstraintViolationException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles data integrity violation exceptions
     *
     * @param e DataIntegrityViolationException containing integrity violation information
     * @return ResponseEntity with an ErrorItem object containing:
     *         - Error message
     *         - URL
     *         - Status code
     *         - Timestamp
     *         - HTTP 400 status (BAD_REQUEST)
     */
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<ErrorItem> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    /**
     * Handles {@link HttpMessageNotReadableException} which occurs when HTTP request body
     * cannot be properly parsed or converted to the expected Java object.
     *
     * <p>This typically happens when:
     * <ul>
     *   <li>Malformed JSON syntax in request body</li>
     *   <li>Type mismatch between JSON values and target Java types</li>
     *   <li>Invalid enum values that cannot be converted to the target enum type</li>
     *   <li>Missing required fields in JSON payload</li>
     * </ul>
     *
     * @param e the HttpMessageNotReadableException that was thrown during request processing
     * @return ResponseEntity containing ErrorItem with details about the parsing error
     * @see HttpMessageNotReadableException
     * @see HttpStatus#BAD_REQUEST
     * @since 1.0
     */
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorItem> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }

    @ExceptionHandler({PaymentNotFoundException.class})
    public ResponseEntity<ErrorItem> handlePaymentNotFoundException(PaymentNotFoundException e) {
        ErrorItem error = ErrorItem.generateMessage(e, HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(error.getStatusCode()).body(error);
    }
}