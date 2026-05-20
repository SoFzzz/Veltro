package com.veltro.inventory.listener;

import com.veltro.inventory.event.ProductCreatedEvent;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.service.InventoryService;
import com.veltro.inventory.service.AlertService;
import com.veltro.inventory.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link ProductCreatedEvent} and creates the associated inventory record.
 *
 * <p>This decouples {@code ProductService} from {@code InventoryService}:
 * the product service no longer needs a direct dependency on the inventory service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateInventoryListener {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final AlertService alertService;

    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        ProductEntity product = productRepository.findById(event.productId())
                .orElseThrow(() -> new NotFoundException(
                        "Product not found with id: " + event.productId()));

        inventoryService.createForProduct(product);
        log.info("Inventory record created for new product: id={}", event.productId());
        
        alertService.evaluateStock(product.getId(), product.getBusinessId());
        log.info("Initial stock alert evaluated for new product: id={}", event.productId());
    }
}
