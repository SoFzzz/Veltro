package com.veltro.inventory.exception;

/**
 * Conflict raised when a product creation request collides with an existing product.
 *
 * <p>Includes the existing product identifier when available so API consumers
 * can redirect to the existing resource.
 */
public class DuplicateProductConflictException extends RuntimeException {

    private final Long existingProductId;

    public DuplicateProductConflictException(Long existingProductId, Throwable cause) {
        super("Duplicate product conflict", cause);
        this.existingProductId = existingProductId;
    }

    public Long getExistingProductId() {
        return existingProductId;
    }
}
