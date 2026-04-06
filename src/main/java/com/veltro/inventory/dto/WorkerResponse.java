package com.veltro.inventory.dto;

import java.time.Instant;

/**
 * Response DTO for a worker/employee.
 * Excludes sensitive fields like passwordHash.
 */
public record WorkerResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean active,
        Instant createdAt
) {
}
