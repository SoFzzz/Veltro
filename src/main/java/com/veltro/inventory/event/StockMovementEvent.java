package com.veltro.inventory.event;

import com.veltro.inventory.model.MovementType;
import java.time.OffsetDateTime;

public record StockMovementEvent(
        Long businessId,
        Long productId,
        Long movementId,
        MovementType movementType,
        int previousStock,
        int newStock,
        OffsetDateTime occurredAt) {
}

