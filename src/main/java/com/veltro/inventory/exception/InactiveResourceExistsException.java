package com.veltro.inventory.exception;

/**
 * Thrown when attempting to create a resource that already exists but is inactive/soft-deleted.
 * Suggests reactivating the existing resource instead of creating a duplicate.
 * Maps to HTTP 409 Conflict with a specific error code.
 */
public class InactiveResourceExistsException extends RuntimeException {

    private final Long existingResourceId;
    private final String resourceType;

    public InactiveResourceExistsException(String resourceType, String field, String value, Long existingResourceId) {
        super(String.format(
                "An inactive %s with %s '%s' already exists (id=%d). Consider reactivating it instead of creating a new one.",
                resourceType, field, value, existingResourceId));
        this.existingResourceId = existingResourceId;
        this.resourceType = resourceType;
    }

    public Long getExistingResourceId() {
        return existingResourceId;
    }

    public String getResourceType() {
        return resourceType;
    }
}
