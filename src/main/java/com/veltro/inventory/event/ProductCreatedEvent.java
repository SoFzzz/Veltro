package com.veltro.inventory.event;

/**
 * Domain event published when a new product is created.
 *
 * <p>Decouples {@code ProductService} from {@code InventoryService}:
 * instead of calling {@code inventoryService.createForProduct()} directly,
 * {@code ProductService} publishes this event and a listener creates the
 * inventory record asynchronously within the same transaction.
 *
 * @param productId  the new product's ID
 * @param businessId the tenant's business ID
 */
public record ProductCreatedEvent(
        Long productId,
        Long businessId
) {}
