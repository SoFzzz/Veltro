package com.veltro.inventory.exception;

import lombok.Getter;

/**
 * Thrown when a stock entry would push inventory above the configured maximum stock limit (BUG-11).
 * Maps to HTTP 422 Unprocessable Entity.
 *
 * Example: adding 50 units when current stock is 80 and maxStock is 100.
 */
@Getter
public class MaxStockExceededException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageArgs;

    public MaxStockExceededException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }

    public MaxStockExceededException(String productName, int currentStock, int quantity, int maxStock) {
        super(String.format(
                "Stock entry would exceed maximum stock limit for '%s': current %d + entry %d = %d, max allowed %d.",
                productName, currentStock, quantity, currentStock + quantity, maxStock));
        this.messageKey = "error.max_stock_exceeded";
        this.messageArgs = new Object[]{productName, currentStock, quantity, currentStock + quantity, maxStock};
    }
}
