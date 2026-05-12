package com.veltro.inventory.service;

import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.event.SaleVoidedEvent;
import com.veltro.inventory.model.SaleEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for creating sale-related domain events.
 *
 * <p>Extracted from {@code SaleService} to separate event-mapping logic
 * from business coordination.
 */
@Component
public class SaleEventFactory {

    /**
     * Builds a {@link SaleCompletedEvent} from a completed sale entity.
     */
    public SaleCompletedEvent buildCompletedEvent(SaleEntity sale) {
        List<SaleItemInfo> items = mapDetails(sale);

        return new SaleCompletedEvent(
                sale.getBusinessId(),
                sale.getId(),
                sale.getSaleNumber(),
                sale.getCashierId(),
                sale.getTotal(),
                sale.getPaymentMethod(),
                sale.getCompletedAt(),
                items
        );
    }

    /**
     * Builds a {@link SaleVoidedEvent} from a voided sale entity.
     */
    public SaleVoidedEvent buildVoidedEvent(SaleEntity sale) {
        List<SaleItemInfo> items = mapDetails(sale);
        String voidedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        return new SaleVoidedEvent(
                sale.getBusinessId(),
                sale.getId(),
                sale.getSaleNumber(),
                voidedBy,
                LocalDateTime.now(),
                sale.getTotal(),
                items
        );
    }

    private List<SaleItemInfo> mapDetails(SaleEntity sale) {
        return sale.getDetails().stream()
                .filter(d -> d.isActive())
                .map(d -> new SaleItemInfo(
                        d.getId(),
                        d.getProductId(),
                        d.getQuantity(),
                        d.getUnitPrice(),
                        d.getSubtotal()
                ))
                .collect(Collectors.toList());
    }
}
