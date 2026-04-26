package com.veltro.inventory.exception.ai;

public class VectorDatabaseException extends RuntimeException {
    public VectorDatabaseException(String message) {
        super(message);
    }
    public VectorDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
