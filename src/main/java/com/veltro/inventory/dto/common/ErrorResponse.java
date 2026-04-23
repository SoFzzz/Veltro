package com.veltro.inventory.dto.common;

import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Immutable error payload returned by the API for all error responses.
 */
public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path
) {
    public static ErrorResponse of(String code, String message, HttpStatus status, String path) {
        return new ErrorResponse(code, message, status.value(), Instant.now(), path);
    }
}
