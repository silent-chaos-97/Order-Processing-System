package com.winter.orderprocessing.exception;

// Thrown when a requested entity cannot be found
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
