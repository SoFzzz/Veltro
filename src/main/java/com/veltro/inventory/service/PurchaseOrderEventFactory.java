package com.veltro.inventory.service;

import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.event.ReceivedItemInfo;
import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for creating purchase order-related domain events.
 */
@Component
public class PurchaseOrderEventFactory {

    public OrderReceivedEvent buildReceivedEvent(PurchaseOrderEntity order, List<PurchaseOrderDetailEntity> receivedDetails) {
        List<ReceivedItemInfo> receivedItems = receivedDetails.stream()
                .map(detail -> new ReceivedItemInfo(
                        detail.getId(),
                        detail.getProduct().getId(),
                        detail.getReceivedQuantity(),
                        detail.getUnitCost(),
                        detail.getUnitCost().multiply(BigDecimal.valueOf(detail.getReceivedQuantity()))
                ))
                .collect(Collectors.toList());

        String receivedBy = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "System";

        return new OrderReceivedEvent(
                order.getBusinessId(),
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getCompanyName(),
                order.getTotal(),
                LocalDateTime.now(),
                receivedBy,
                receivedItems
        );
    }
}
