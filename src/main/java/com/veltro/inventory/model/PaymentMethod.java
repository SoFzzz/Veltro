package com.veltro.inventory.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Payment method types for sale confirmation (B2-01).
 */
public enum PaymentMethod {
    /**
     * Cash payment - requires amountReceived to calculate change.
     */
    CASH,

    /**
     * Card payment (credit/debit).
     */
    CARD,

    /**
     * Yape mobile payment.
     */
    YAPE,

    /**
     * Plin mobile payment.
     */
    PLIN,

    /**
     * Generic bank transfer / wallet transfer.
     */
    TRANSFER,

    /**
     * Mixed payment (multiple methods in one sale).
     */
    MIXED;

    /**
     * Accepts legacy and localized aliases from clients.
     *
     * <p>Examples mapped here:
     * <ul>
     *   <li>NEQUI, DAVIPLATA -> TRANSFER</li>
     *   <li>TARJETA -> CARD</li>
     *   <li>EFECTIVO -> CASH</li>
     * </ul>
     */
    @JsonCreator
    public static PaymentMethod fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = normalize(rawValue);
        return switch (normalized) {
            case "CASH", "EFECTIVO" -> CASH;
            case "CARD", "TARJETA" -> CARD;
            case "YAPE" -> YAPE;
            case "PLIN" -> PLIN;
            case "TRANSFER", "NEQUI", "DAVIPLATA", "DAVI_PLATA" -> TRANSFER;
            case "MIXED", "MIXTO" -> MIXED;
            default -> throw new IllegalArgumentException("Unsupported payment method: " + rawValue);
        };
    }

    private static String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
