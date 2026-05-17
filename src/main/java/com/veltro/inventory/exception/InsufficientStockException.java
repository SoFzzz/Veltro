package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when a sale or adjustment would push stock below zero (CA-04).
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * Example: selling 10 units of a product that only has 5 in stock.
 */
@Getter
public class InsufficientStockException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public InsufficientStockException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public InsufficientStockException(String productName, int available, int requested) {
        super("Insufficient stock for '" + productName + "': available " + available
                + ", requested " + requested + ".");
        this.messageKey = "error.insufficient_stock";
        this.messageArgs = new Object[]{productName, available, requested};
    }
}
