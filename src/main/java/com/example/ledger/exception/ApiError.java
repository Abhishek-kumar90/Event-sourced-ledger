package com.example.ledger.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform, safe error body. No stack traces or internal details ever reach the client
 * (Security Baseline: never expose internals; fail closed).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
