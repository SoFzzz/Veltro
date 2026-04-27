package com.veltro.inventory.service;

import com.veltro.inventory.model.SaleDetailEntity;
import com.veltro.inventory.model.SaleEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service dedicated to capturing immutable snapshots of sale state for auditing (B3-03).
 *
 * <p>Extracted from {@code SaleService} to reduce class size and separate
 * auditing concerns from business logic.
 */
@Service
public class SaleSnapshotService {

    /**
     * Builds a snapshot map of sale state for forensic audit.
     *
     * @param sale the sale entity to snapshot
     * @return map containing sale state for audit record
     */
    public Map<String, Object> buildSnapshot(SaleEntity sale) {
        return AuditSnapshotBuilder.create()
                .put("id", sale.getId())
                .put("saleNumber", sale.getSaleNumber())
                .putEnum("status", sale.getStatus())
                .put("cashierId", sale.getCashierId())
                .put("subtotal", sale.getSubtotal())
                .put("total", sale.getTotal())
                .putEnum("paymentMethod", sale.getPaymentMethod())
                .put("amountReceived", sale.getAmountReceived())
                .put("change", sale.getChange())
                .putTemporal("completedAt", sale.getCompletedAt())
                .withDetails(
                        sale.getDetails().stream()
                                .filter(SaleDetailEntity::isActive)
                                .collect(Collectors.toList()),
                        d -> AuditSnapshotBuilder.create()
                                .put("id", d.getId())
                                .put("productId", d.getProductId())
                                .put("productName", d.getProductName())
                                .put("quantity", d.getQuantity())
                                .put("unitPrice", d.getUnitPrice())
                                .put("subtotal", d.getSubtotal())
                                .build()
                )
                .build();
    }
}
