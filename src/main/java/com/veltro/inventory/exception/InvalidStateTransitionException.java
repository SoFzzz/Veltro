package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when a state transition is requested but is not allowed
 * given the entity's current state (ADR-006 – State Pattern).
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * Example: attempting to confirm a sale that is already COMPLETED.
 */
@Getter
public class InvalidStateTransitionException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidStateTransitionException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    @Deprecated
    public InvalidStateTransitionException(String entityName, String currentState, String requestedAction) {
        super(entityName + " is already in state " + currentState
                + ". Action '" + requestedAction + "' is not allowed.");
        this.messageKey = null;
        this.messageArgs = null;
    }

    public InvalidStateTransitionException(String messageKey, Object[] args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.messageArgs = args;
    }
}
