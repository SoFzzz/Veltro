package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when attempting to create a resource that already exists but is inactive/soft-deleted.
 * Suggests reactivating the existing resource instead of creating a duplicate.
 * Maps to HTTP 409 Conflict with a specific error code.
 */
@Getter
public class InactiveResourceExistsException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;
    private final Long existingResourceId;
    private final String resourceType;

    public InactiveResourceExistsException(String resourceType, String field, String value, Long existingResourceId) {
        super(String.format(
                "An inactive %s with %s '%s' already exists (id=%d). Consider reactivating it instead of creating a new one.",
                resourceType, field, value, existingResourceId));
        this.messageKey = "error.inactive_resource_exists";
        this.messageArgs = new Object[]{resourceType, field, value, existingResourceId};
        this.existingResourceId = existingResourceId;
        this.resourceType = resourceType;
    }
}
