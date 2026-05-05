package com.veltro.inventory.dto.common;

public record DuplicateProductErrorResponse(
        String message,
        Long existingProductId
) {
}
