package com.veltro.inventory.exception;

/**
 * Thrown when a product's salePrice is less than its costPrice.
 *
 * Mapped to HTTP 422 Unprocessable Content by {@code GlobalExceptionHandler}.
 */
public class InvalidPriceException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidPriceException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public InvalidPriceException(String message, String messageKey, Object... messageArgs) {
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
