package com.veltro.inventory.service;

import java.time.LocalDateTime;

/**
 * Utility class for generating order/sale numbers with a consistent format.
 *
 * <p>Centralises the number-generation logic previously duplicated across
 * {@code SaleService} (VLT-YYYY-NNNNNN) and {@code PurchaseOrderService} (PO-YYYY-NNNNNN).
 */
public final class OrderNumberGenerator {

    private OrderNumberGenerator() {
        // utility class
    }

    /**
     * Generates a number in the format {@code PREFIX-YYYY-NNNNNN}.
     *
     * @param prefix        the business prefix (e.g. "VLT" for sales, "PO" for purchase orders)
     * @param sequenceValue the sequence value to pad
     * @return formatted order number
     */
    public static String generate(String prefix, Long sequenceValue) {
        int year = LocalDateTime.now().getYear();
        return String.format("%s-%d-%06d", prefix, year, sequenceValue);
    }
}
