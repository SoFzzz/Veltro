package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.InventoryMovementResponse;
import com.veltro.inventory.dto.inventory.InventoryResponse;
import com.veltro.inventory.dto.inventory.StockAdjustmentRequest;
import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.dto.inventory.StockExitRequest;
import com.veltro.inventory.dto.inventory.UpdateStockLimitsRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.event.StockChangedEvent;
import com.veltro.inventory.mapper.InventoryMapper;
import com.veltro.inventory.mapper.InventoryMovementMapper;
import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.model.InventoryEntity;
import com.veltro.inventory.model.InventoryMovementEntity;
import com.veltro.inventory.model.MovementType;
import com.veltro.inventory.repository.InventoryMovementRepository;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.exception.InsufficientStockException;
import com.veltro.inventory.exception.MaxStockExceededException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for inventory management (B1-04).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryMovementMapper movementMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditCommandExecutor auditCommandExecutor;
    private final com.veltro.inventory.repository.AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> findAll(Pageable pageable) {
        Long businessId = TenantContext.getBusinessId();
        return PageResponse.from(
                inventoryRepository.findAllByActiveTrueAndBusinessId(businessId, pageable)
                        .map(inventoryMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public InventoryResponse findByProductId(Long productId) {
        Long businessId = TenantContext.getBusinessId();
        return inventoryMapper.toResponse(requireByProductId(productId, businessId));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponse> getMovements(Long productId, Pageable pageable) {
        Long businessId = TenantContext.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        return PageResponse.from(
                movementRepository.findByInventoryIdAndBusinessId(inventory.getId(), businessId, pageable)
                        .map(movementMapper::toResponse)
        );
    }

    @Transactional
    public InventoryResponse recordEntry(Long productId, StockEntryRequest request) {
        Long businessId = TenantContext.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();
        int newStock = previousStock + request.quantity();

        // BUG-11: Validate max stock limit (only if maxStock is configured > 0)
        int maxStock = inventory.getMaxStock();
        if (maxStock > 0 && newStock > maxStock) {
            throw new MaxStockExceededException(
                    inventory.getProduct().getName(), previousStock, request.quantity(), maxStock);
        }

        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.ENTRY, request.quantity(), previousStock, newStock, request.reason());
        publishStockChanged(saved, previousStock, newStock, request.reason());

        log.info("Stock ENTRY: productId={}, qty={}, stock {} -> {}",
                productId, request.quantity(), previousStock, newStock);
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse recordExit(Long productId, StockExitRequest request) {
        Long businessId = TenantContext.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();

        if (previousStock - request.quantity() < 0) {
            throw new InsufficientStockException(
                    inventory.getProduct().getName(), previousStock, request.quantity());
        }

        int newStock = previousStock - request.quantity();
        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.EXIT, request.quantity(), previousStock, newStock, request.reason());
        publishStockChanged(saved, previousStock, newStock, request.reason());

        log.info("Stock EXIT: productId={}, qty={}, stock {} -> {}",
                productId, request.quantity(), previousStock, newStock);
        return inventoryMapper.toResponse(saved);
    }

    /**
     * Records an inventory adjustment with forensic audit logging (B3-03).
     *
     * <p>Captures before/after state for audit trail and publishes StockChangedEvent
     * for downstream alert evaluation.
     *
     * @param productId the product ID
     * @param request   the adjustment details (newStock, reason)
     * @return the updated inventory
     */
    @Transactional
    public InventoryResponse recordAdjustment(Long productId, StockAdjustmentRequest request) {
        Long businessId = TenantContext.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();
        int newStock = request.newStock();
        int delta = Math.abs(newStock - previousStock);
        int quantityForRecord = delta == 0 ? 1 : delta;

        // Capture state BEFORE adjustment for audit (B3-03)
        final Map<String, Object> beforeSnapshot = buildInventorySnapshot(inventory);

        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.ADJUSTMENT, quantityForRecord, previousStock, newStock, request.reason());
        publishStockChanged(saved, previousStock, newStock, request.reason());

        // Create forensic audit record (B3-03)
        auditCommandExecutor.execute(
                AuditEntityType.INVENTORY,
                saved.getId(),
                AuditAction.ADJUST,
                () -> beforeSnapshot,
                () -> saved,
                (result) -> buildInventorySnapshot(saved),
                RequestAuditContext.empty()
        );

        log.info("Stock ADJUSTMENT: productId={}, stock {} -> {}",
                productId, previousStock, newStock);
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse updateLimits(Long productId, UpdateStockLimitsRequest request) {
        Long businessId = TenantContext.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        inventory.setMinStock(request.minStock());
        inventory.setMaxStock(request.maxStock());
        InventoryEntity saved = inventoryRepository.save(inventory);

        log.info("Stock limits updated: productId={}, min={}, max={}",
                productId, request.minStock(), request.maxStock());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryEntity createForProduct(ProductEntity product) {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setProduct(product);
        inventory.setBusinessId(product.getBusinessId());
        inventory.setCurrentStock(0);
        inventory.setMinStock(0);
        inventory.setMaxStock(0);
        InventoryEntity saved = inventoryRepository.save(inventory);
        log.info("Inventory record created: productId={}, inventoryId={}", product.getId(), saved.getId());
        return saved;
    }

    private InventoryEntity requireByProductId(Long productId, Long businessId) {
        return inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for product id: " + productId));
    }

    private void persistMovement(InventoryEntity inventory, MovementType type,
                                 int quantity, int previousStock, int newStock, String reason) {
         Long businessId = TenantContext.getBusinessId();
         InventoryMovementEntity movement = new InventoryMovementEntity();
         movement.setInventory(inventory);
         movement.setMovementType(type);
         movement.setQuantity(quantity);
         movement.setPreviousStock(previousStock);
         movement.setNewStock(newStock);
         movement.setReason(reason);
         movement.setBusinessId(businessId);
         movementRepository.save(movement);

         // Generate INFO alert for the movement
         com.veltro.inventory.model.AlertEntity alert = new com.veltro.inventory.model.AlertEntity();
         alert.setProduct(inventory.getProduct());
         alert.setType(com.veltro.inventory.model.AlertType.STOCK_MOVEMENT);
         alert.setSeverity(com.veltro.inventory.model.AlertSeverity.INFO);
         String action = type == MovementType.ENTRY ? "Llegada" : (type == MovementType.EXIT ? "Salida" : "Ajuste");
         alert.setMessage(String.format("Registro de %s: %s (Cambio de %d a %d)", action, inventory.getProduct().getName(), previousStock, newStock));
         alert.setBusinessId(businessId);
         alertRepository.save(alert);
    }

    private void publishStockChanged(InventoryEntity inventory, int previousStock, int newStock, String reason) {
        StockChangedEvent event = new StockChangedEvent(
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                previousStock,
                newStock,
                reason,
                OffsetDateTime.now());
        eventPublisher.publishEvent(event);
    }

    /**
     * Builds a snapshot map of inventory state for forensic audit (B3-03).
     *
     * @param inventory the inventory entity to snapshot
     * @return map containing inventory state for audit record
     */
    private Map<String, Object> buildInventorySnapshot(InventoryEntity inventory) {
        return AuditSnapshotBuilder.create()
                .put("id", inventory.getId())
                .put("productId", inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .put("productName", inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                .put("currentStock", inventory.getCurrentStock())
                .put("minStock", inventory.getMinStock())
                .put("maxStock", inventory.getMaxStock())
                .build();
    }
}


