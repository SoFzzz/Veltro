package com.veltro.inventory.application.shared.dto;

import com.veltro.inventory.model.AbstractAuditableEntity;

import java.time.LocalDateTime;

/**
 * Shared audit information DTO for all responses that include audit fields (B2-01).
 *
 * <p>This record is reusable across all modules that extend {@link AbstractAuditableEntity}.
 */
public record AuditInfo(
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
}
