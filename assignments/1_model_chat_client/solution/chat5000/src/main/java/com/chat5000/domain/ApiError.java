package com.chat5000.domain;

import java.time.Instant;

/**
 * Standard JSON error payload returned by filters and exception handlers.
 */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now());
    }
}
