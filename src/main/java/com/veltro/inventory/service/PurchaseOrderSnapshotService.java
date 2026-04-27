package com.veltro.inventory.service;

import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service dedicated to capturing immutable snapshots of purchase order state for auditing (B3-03).
 */
@Service
public class PurchaseOrderSnapshotService {

    public Map<String, Object> buildSnapshot(PurchaseOrderEntity order) {
        return AuditSnapshotBuilder.create()
                .put("id", order.getId())
                .put("orderNumber", order.getOrderNumber())
                .putEnum("status", order.getStatus())
                .put("supplierId", order.getSupplier() != null ? order.getSupplier().getId() : null)
                .put("supplierName", order.getSupplier() != null ? order.getSupplier().getCompanyName() : null)
                .put("total", order.getTotal())
                .put("notes", order.getNotes())
                .withDetails(
                        order.getDetails().stream()
                                .filter(PurchaseOrderDetailEntity::isActive)
                                .collect(Collectors.toList()),
                        d -> AuditSnapshotBuilder.create()
                                .put("id", d.getId())
                                .put("productId", d.getProduct() != null ? d.getProduct().getId() : null)
                                .put("productName", d.getProduct() != null ? d.getProduct().getName() : null)
                                .put("requestedQuantity", d.getRequestedQuantity())
                                .put("receivedQuantity", d.getReceivedQuantity())
                                .put("unitCost", d.getUnitCost())
                                .build()
                )
                .build();
    }
}
