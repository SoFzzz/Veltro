package com.veltro.inventory.exception.ai;

public class ClipModelInferenceException extends RuntimeException {
    public ClipModelInferenceException(String message) {
        super(message);
    }
    public ClipModelInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
