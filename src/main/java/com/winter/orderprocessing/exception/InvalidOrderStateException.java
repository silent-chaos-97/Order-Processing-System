package com.winter.orderprocessing.exception;

// Thrown when an operation is not allowed given the order's current status
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
