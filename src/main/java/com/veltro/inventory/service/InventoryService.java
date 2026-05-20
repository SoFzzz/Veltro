package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.InventoryMovementResponse;
import com.veltro.inventory.dto.inventory.InventoryResponse;
import com.veltro.inventory.dto.inventory.StockAdjustmentRequest;
import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.dto.inventory.StockExitRequest;
import com.veltro.inventory.dto.inventory.UpdateStockLimitsRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.event.StockMovementEvent;
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
import com.veltro.inventory.security.TenantProvider;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final com.veltro.inventory.repository.AlertConfigurationRepository configurationRepository;
    private final MessageSource messageSource;
    private final TenantProvider tenantProvider;
    private final AlertService alertService;

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> findAll(Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        return PageResponse.from(
                inventoryRepository.findAllByActiveTrueAndBusinessId(businessId, pageable)
                        .map(inventoryMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public InventoryResponse findByProductId(Long productId) {
        Long businessId = tenantProvider.getBusinessId();
        return inventoryMapper.toResponse(requireByProductId(productId, businessId));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryMovementResponse> getMovements(Long productId, Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        return PageResponse.from(
                movementRepository.findByInventoryIdAndBusinessId(inventory.getId(), businessId, pageable)
                        .map(movementMapper::toResponse)
        );
    }

    @Transactional
    public InventoryResponse recordEntry(Long productId, StockEntryRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        return recordEntry(productId, request.quantity(), request.reason(), businessId, null, null);
    }

    @Transactional
    public InventoryResponse recordEntry(Long productId, StockEntryRequest request, Long businessId, String sourceType, Long sourceId) {
        return recordEntry(productId, request.quantity(), request.reason(), businessId, sourceType, sourceId);
    }

    @Transactional
    public InventoryResponse recordEntry(Long productId, int quantity, String reason, Long businessId,
                                         String sourceType, Long sourceId) {
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();
        int newStock = previousStock + quantity;

        // BUG-11: Validate max stock limit (only if maxStock is configured > 0)
        int maxStock = inventory.getMaxStock();
        if (maxStock > 0 && newStock > maxStock) {
            throw new MaxStockExceededException(
                    inventory.getProduct().getName(), previousStock, quantity, maxStock);
        }

        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.ENTRY, quantity, previousStock, newStock, reason, businessId,
                sourceType, sourceId);

        log.info("Stock ENTRY: productId={}, qty={}, stock {} -> {}",
                productId, quantity, previousStock, newStock);
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse recordExit(Long productId, StockExitRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        return recordExit(productId, request.quantity(), request.reason(), businessId, null, null);
    }

    @Transactional
    public InventoryResponse recordExit(Long productId, int quantity, String reason, Long businessId,
                                        String sourceType, Long sourceId) {
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();

        if (previousStock - quantity < 0) {
            throw new InsufficientStockException(
                    inventory.getProduct().getName(), previousStock, quantity);
        }

        int newStock = previousStock - quantity;
        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.EXIT, quantity, previousStock, newStock, reason, businessId,
                sourceType, sourceId);

        log.info("Stock EXIT: productId={}, qty={}, stock {} -> {}",
                productId, quantity, previousStock, newStock);
        return inventoryMapper.toResponse(saved);
    }

    /**
     * Records an inventory adjustment with forensic audit logging (B3-03).
     *
     * <p>Captures before/after state for audit trail and publishes StockMovementEvent
     * for downstream alert evaluation.
     *
     * @param productId the product ID
     * @param request   the adjustment details (newStock, reason)
     * @return the updated inventory
     */
    @Transactional
    public InventoryResponse recordAdjustment(Long productId, StockAdjustmentRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        int previousStock = inventory.getCurrentStock();
        int newStock = request.newStock();
        int delta = Math.abs(newStock - previousStock);
        int quantityForRecord = delta == 0 ? 1 : delta;

        // Capture state BEFORE adjustment for audit (B3-03)
        final Map<String, Object> beforeSnapshot = buildInventorySnapshot(inventory);

        inventory.setCurrentStock(newStock);
        InventoryEntity saved = inventoryRepository.save(inventory);
        persistMovement(saved, MovementType.ADJUSTMENT, quantityForRecord, previousStock, newStock, request.reason(), businessId,
                null, null);

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
        Long businessId = tenantProvider.getBusinessId();
        InventoryEntity inventory = requireByProductId(productId, businessId);
        inventory.setMinStock(request.minStock());
        inventory.setMaxStock(request.maxStock());
        InventoryEntity saved = inventoryRepository.save(inventory);

        configurationRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .ifPresentOrElse(config -> {
                    config.setMinStock(request.minStock());
                    config.setOverstockThreshold(request.maxStock());
                    configurationRepository.save(config);
                }, () -> {
                    com.veltro.inventory.model.AlertConfigurationEntity config = new com.veltro.inventory.model.AlertConfigurationEntity();
                    config.setProduct(inventory.getProduct());
                    config.setBusinessId(businessId);
                    config.setCriticalStock(0);
                    config.setMinStock(request.minStock());
                    config.setOverstockThreshold(request.maxStock());
                    configurationRepository.save(config);
                });

        log.info("Stock limits updated: productId={}, min={}, max={}",
                productId, request.minStock(), request.maxStock());
        alertService.evaluateStock(productId, businessId);
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
                                 int quantity, int previousStock, int newStock, String reason, Long businessId,
                                 String sourceType, Long sourceId) {
         InventoryMovementEntity movement = new InventoryMovementEntity();
         movement.setInventory(inventory);
         movement.setMovementType(type);
         movement.setQuantity(quantity);
         movement.setPreviousStock(previousStock);
         movement.setNewStock(newStock);
         movement.setReason(reason);
         movement.setBusinessId(businessId);
         movement.setSourceType(sourceType);
         movement.setSourceId(sourceId);
         InventoryMovementEntity savedMovement = movementRepository.save(movement);

         // Generate INFO alert for the movement
         com.veltro.inventory.model.AlertEntity alert = new com.veltro.inventory.model.AlertEntity();
         alert.setProduct(inventory.getProduct());
         alert.setType(com.veltro.inventory.model.AlertType.STOCK_MOVEMENT);
         alert.setSeverity(com.veltro.inventory.model.AlertSeverity.INFO);
         alert.setMessage(resolveMovementMessage(type, inventory.getProduct().getName(), previousStock, newStock));
         alert.setBusinessId(businessId);
         alert.setMovementId(savedMovement.getId());
         alertRepository.save(alert);
         publishStockMovement(savedMovement, previousStock, newStock, businessId);
    }

    private String resolveMovementMessage(MovementType movementType, String productName, int previousStock, int newStock) {
        String key = switch (movementType) {
            case ENTRY -> "alert.movement.entry";
            case EXIT -> "alert.movement.exit";
            case ADJUSTMENT -> "alert.movement.adjustment";
        };
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, new Object[]{productName, previousStock, newStock}, locale);
    }

    private void publishStockMovement(InventoryMovementEntity movement, int previousStock, int newStock, Long businessId) {
        StockMovementEvent event = new StockMovementEvent(
                businessId,
                movement.getInventory().getProduct().getId(),
                movement.getId(),
                movement.getMovementType(),
                previousStock,
                newStock,
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


