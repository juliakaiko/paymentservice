package com.mymicroservice.paymentservice.exception;

import java.util.NoSuchElementException;

public class PaymentNotFoundException extends NoSuchElementException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
