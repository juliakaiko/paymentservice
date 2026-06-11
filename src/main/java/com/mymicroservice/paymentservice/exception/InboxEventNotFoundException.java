package com.mymicroservice.paymentservice.exception;

import java.util.NoSuchElementException;

public class InboxEventNotFoundException extends NoSuchElementException {

    public InboxEventNotFoundException(String message) {
        super(message);
    }
}
