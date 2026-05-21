package com.veltro.inventory.exception;

import com.veltro.inventory.service.SaleService;

/**
 * Exception thrown when payment details are invalid (B2-01).
 *
 * <p>Thrown by {@link SaleService#confirm}
 * when cash payment validation fails (amount received is null or less than total).
 *
 * <p>Mapped to HTTP 422 UNPROCESSABLE_ENTITY with error code "INVALID_PAYMENT"
 * by {@link com.veltro.inventory.infrastructure.adapters.web.GlobalExceptionHandler}.
 */
public class InvalidPaymentException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidPaymentException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public InvalidPaymentException(String message, String messageKey, Object... messageArgs) {
        super(message);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs != null ? messageArgs.clone() : new Object[0];
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }
}
