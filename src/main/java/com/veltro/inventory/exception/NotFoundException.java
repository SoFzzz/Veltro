package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when a requested resource does not exist or is not visible
 * (e.g. soft-deleted). Maps to HTTP 404 Not Found.
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public NotFoundException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public NotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
        this.messageKey = "error.not_found.by_id";
        this.messageArgs = new Object[]{resourceName, id};
    }

    public NotFoundException(String resourceName, String field, String value) {
        super(resourceName + " not found with " + field + ": " + value);
        this.messageKey = "error.not_found";
        this.messageArgs = new Object[]{resourceName, field, value};
    }
}
