package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when attempting to create a resource that already exists 
 * (e.g., duplicate tax ID, barcode, etc.). Maps to HTTP 409 Conflict.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public DuplicateResourceException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public DuplicateResourceException(String resourceName, String field, String value) {
        super(resourceName + " with " + field + " '" + value + "' already exists");
        this.messageKey = "error.duplicate_resource";
        this.messageArgs = new Object[]{resourceName, field, value};
    }
}
