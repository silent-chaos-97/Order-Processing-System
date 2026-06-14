package com.winter.orderprocessing.exception;

import java.time.Instant;
import java.util.Map;

// Standard error body returned by the global exception handler
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    // Convenience constructor for errors without field-level details
    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    // Convenience constructor for validation errors with field-level details
    public ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
        this(Instant.now(), status, error, message, fieldErrors);
    }
}
