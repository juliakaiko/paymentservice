package com.mymicroservice.paymentservice.exception;

import java.util.NoSuchElementException;

public class InboxEventNotFound extends NoSuchElementException {

    public InboxEventNotFound(String message) {
        super(message);
    }
}
