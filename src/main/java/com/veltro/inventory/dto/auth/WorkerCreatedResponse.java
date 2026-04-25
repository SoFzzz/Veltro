package com.veltro.inventory.dto.auth;

import java.time.Instant;

/**
 * Response DTO for newly created worker.
 * Excludes sensitive fields like passwordHash.
 */
public record WorkerCreatedResponse(
        Long id,
        String username,
        String email,
        String role,
        Instant createdAt
) {
}