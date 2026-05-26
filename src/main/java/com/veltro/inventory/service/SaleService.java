package com.veltro.inventory.service;

import com.veltro.inventory.dto.pos.AddItemRequest;
import com.veltro.inventory.dto.pos.ConfirmSaleRequest;
import com.veltro.inventory.dto.pos.ModifyItemRequest;
import com.veltro.inventory.dto.pos.QuickSaleRequest;
import com.veltro.inventory.dto.pos.SaleResponse;
import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.event.SaleVoidedEvent;
import com.veltro.inventory.mapper.SaleMapper;
import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.model.InventoryEntity;
import com.veltro.inventory.model.PaymentMethod;
import com.veltro.inventory.model.SaleDetailEntity;
import com.veltro.inventory.model.SaleEntity;
import com.veltro.inventory.model.SaleStatus;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.repository.SaleRepository;
import com.veltro.inventory.exception.InvalidPaymentException;
import com.veltro.inventory.exception.InsufficientStockException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.veltro.inventory.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Application service for sale (POS) management (B2-01).
 *
 * <p>Owns all {@link Transactional} boundaries for sale operations.
 * Enforces the State Pattern (ADR-006) via delegation to {@link SaleEntity}.
 * Publishes domain events for downstream listeners (B2-02).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleMapper saleMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditCommandExecutor auditCommandExecutor;
    private final SaleSnapshotService snapshotService;
    private final SaleEventFactory eventFactory;
    private final TenantProvider tenantProvider;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated list of sales for the current business, newest first (V04).
     *
     * @param status   optional status filter (null = all statuses)
     * @param pageable pagination info (sort is overridden to id DESC)
     */
    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> findAll(SaleStatus status, Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        // Always force newest-first regardless of what the caller sends
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        if (status != null) {
            return PageResponse.from(
                    saleRepository.findAllByActiveTrueAndBusinessIdAndStatus(businessId, status, sorted)
                            .map(saleMapper::toResponse)
            );
        }
        return PageResponse.from(
                saleRepository.findAllByActiveTrueAndBusinessId(businessId, sorted)
                        .map(saleMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long saleId) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));
        return saleMapper.toResponse(sale);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Starts a new sale (IN_PROGRESS status).
     *
     * @return the created sale with generated sale number
     */
    @Transactional
    public SaleResponse startSale() {
        Long userId = getCurrentUserId();
        Long businessId = tenantProvider.getBusinessId();
        Long sequenceValue = saleRepository.getNextSaleSequenceValue();
        String saleNumber = generateSaleNumber(sequenceValue);

        SaleEntity sale = new SaleEntity();
        sale.setSaleNumber(saleNumber);
        sale.setStatus(SaleStatus.IN_PROGRESS);
        sale.setCashierId(userId);
        sale.setBusinessId(businessId);
        sale.setSubtotal(BigDecimal.ZERO);
        sale.setTotal(BigDecimal.ZERO);

        SaleEntity saved = saleRepository.save(sale);
        log.info("Sale started: {} by user {}", saleNumber, userId);
        return saleMapper.toResponse(saved);
    }

    /**
     * Adds an item to the sale cart.
     *
     * @param saleId  the sale ID
     * @param request the item to add (productId, quantity)
     * @return the updated sale
     */
    @Transactional
    public SaleResponse addItem(Long saleId, AddItemRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        ProductEntity product = productRepository.findByIdAndActiveTrueAndBusinessId(request.productId(), businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.productId()));

        // Create detail with product snapshot
        SaleDetailEntity detail = new SaleDetailEntity();
        detail.setProductId(product.getId());
        detail.setProductName(product.getName());
        detail.setQuantity(request.quantity());
        detail.setUnitPrice(product.getSalePrice());
        detail.calculateSubtotal();

        // State pattern validates and adds
        sale.addItem(detail);
        sale.recalculateTotals();

        SaleEntity saved = saleRepository.save(sale);
        log.info("Item added to sale {}: product {} x{}", sale.getSaleNumber(), product.getName(), request.quantity());
        return saleMapper.toResponse(saved);
    }

    /**
     * Modifies the quantity of an existing item in the sale.
     *
     * @param saleId   the sale ID
     * @param detailId the detail ID to modify
     * @param request  the new quantity
     * @return the updated sale
     */
    @Transactional
    public SaleResponse modifyItem(Long saleId, Long detailId, ModifyItemRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        // State pattern validates and modifies
        sale.modifyItem(detailId, request.quantity());
        sale.recalculateTotals();

        SaleEntity saved = saleRepository.save(sale);
        log.info("Item {} modified in sale {}: new quantity {}", detailId, sale.getSaleNumber(), request.quantity());
        return saleMapper.toResponse(saved);
    }

    /**
     * Removes an item from the sale (soft delete via active=false per AC-05).
     *
     * @param saleId   the sale ID
     * @param detailId the detail ID to remove
     * @return the updated sale
     */
    @Transactional
    public SaleResponse removeItem(Long saleId, Long detailId) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        // State pattern validates and soft-deletes
        sale.removeItem(detailId);
        sale.recalculateTotals();

        SaleEntity saved = saleRepository.save(sale);
        log.info("Item {} removed from sale {}", detailId, sale.getSaleNumber());
        return saleMapper.toResponse(saved);
    }

    /**
     * Confirms the sale (transitions to COMPLETED).
     *
     * <p>Validates payment details for CASH transactions (amount received must be >= total).
     * Publishes {@link SaleCompletedEvent} for downstream listeners (B2-02) to handle inventory deduction.
     * Creates forensic audit record (B3-03) capturing before/after state.
     *
     * @param saleId  the sale ID
     * @param request payment method and amount received
     * @return the confirmed sale
     * @throws InvalidPaymentException if cash payment validation fails
     */
    @Transactional
    public SaleResponse confirm(Long saleId, ConfirmSaleRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        // Validate stock availability for all active items in the sale cart before confirming
        Map<Long, Integer> requestedQuantities = new java.util.HashMap<>();
        Map<Long, String> productNames = new java.util.HashMap<>();
        for (SaleDetailEntity item : sale.getDetails()) {
            if (item.isActive()) {
                requestedQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                productNames.put(item.getProductId(), item.getProductName());
            }
        }

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            int requestedQty = entry.getValue();
            String prodName = productNames.get(productId);

            InventoryEntity inventory = inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(
                    productId, businessId)
                    .orElseThrow(() -> new InsufficientStockException(
                            prodName, 0, requestedQty));
            if (inventory.getCurrentStock() < requestedQty) {
                throw new InsufficientStockException(
                        prodName, inventory.getCurrentStock(), requestedQty);
            }
        }

        // Capture state BEFORE confirmation for audit (B3-03)
        final Map<String, Object> beforeSnapshot = snapshotService.buildSnapshot(sale);

        // Validate cash payment (B2-01 requirement)
        if (request.paymentMethod() == PaymentMethod.CASH) {
            if (request.amountReceived() == null) {
                throw new InvalidPaymentException(
                        "Amount received is required for cash payments",
                        "error.payment.amount_required");
            }
            if (request.amountReceived().compareTo(sale.getTotal()) < 0) {
                throw new InvalidPaymentException(
                        "Amount received must be greater than or equal to total for cash payments",
                        "error.payment.insufficient_amount");
            }
            sale.setAmountReceived(request.amountReceived());
            sale.setChange(request.amountReceived().subtract(sale.getTotal()));
        }

        // State pattern validates and transitions
        sale.confirm(request.paymentMethod());

        SaleEntity saved = saleRepository.save(sale);

        // Publish event for listeners (B2-02 will handle inventory deduction)
        applicationEventPublisher.publishEvent(eventFactory.buildCompletedEvent(saved));

        // Create forensic audit record (B3-03)
        auditCommandExecutor.execute(
                AuditEntityType.SALE,
                saved.getId(),
                AuditAction.CONFIRM,
                () -> beforeSnapshot,
                () -> saved,
                (result) -> snapshotService.buildSnapshot(saved),
                RequestAuditContext.empty()
        );

        log.info("Sale {} confirmed with payment method {}", sale.getSaleNumber(), request.paymentMethod());
        return saleMapper.toResponse(saved);
    }

    /**
     * Voids a completed sale (transitions to VOIDED).
     *
     * <p>Only COMPLETED sales can be voided (enforced by State Pattern).
     * Publishes {@link SaleVoidedEvent} for downstream listeners (B2-02) to handle stock reversal.
     * Creates forensic audit record (B3-03) capturing before/after state.
     *
     * @param saleId the sale ID
     * @return the voided sale
     */
    @Transactional
    public SaleResponse voidSale(Long saleId) {
        Long businessId = tenantProvider.getBusinessId();
        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        // Capture state BEFORE voiding for audit (B3-03)
        final Map<String, Object> beforeSnapshot = snapshotService.buildSnapshot(sale);

        // State pattern handles validation (only COMPLETED can be voided)
        sale.voidSale();

        SaleEntity saved = saleRepository.save(sale);

        // Publish event: listener in B2-02 handles stock reversal.
        applicationEventPublisher.publishEvent(eventFactory.buildVoidedEvent(saved));

        // Create forensic audit record (B3-03)
        auditCommandExecutor.execute(
                AuditEntityType.SALE,
                saved.getId(),
                AuditAction.VOID,
                () -> beforeSnapshot,
                () -> saved,
                (result) -> snapshotService.buildSnapshot(saved),
                RequestAuditContext.empty()
        );

        log.info("Sale {} voided successfully", sale.getSaleNumber());
        return saleMapper.toResponse(saved);
    }

    /**
     * Quick sale: starts a sale, adds all items, and confirms in a single transaction.
     *
     * <p>Used by the frontend POS page which submits the entire sale in one shot.
     * Internally delegates to {@link #startSale()}, {@link #addItem}, and {@link #confirm}.
     *
     * @param request the quick sale request (items, paymentMethod, amountReceived, notes)
     * @return the confirmed sale
     */
    @Transactional
    public SaleResponse quickSale(QuickSaleRequest request) {
        SaleResponse started = startSale();
        Long saleId = started.id();
        Long businessId = tenantProvider.getBusinessId();

        SaleEntity sale = saleRepository.findByIdAndActiveTrueAndBusinessId(saleId, businessId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));

        List<Long> requestedProductIds = request.items().stream()
                .map(QuickSaleRequest.Item::productId)
                .toList();
        List<Long> uniqueProductIds = new ArrayList<>(requestedProductIds.stream().distinct().toList());

        Map<Long, ProductEntity> productById = productRepository
                .findAllByIdInAndActiveTrueAndBusinessId(uniqueProductIds, businessId)
                .stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        if (productById.size() != uniqueProductIds.size()) {
            for (Long productId : uniqueProductIds) {
                if (!productById.containsKey(productId)) {
                    throw new NotFoundException("Product not found with id: " + productId);
                }
            }
        }

        Map<Long, Integer> quantitiesByProduct = new HashMap<>();
        for (QuickSaleRequest.Item item : request.items()) {
            quantitiesByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
            ProductEntity product = productById.get(entry.getKey());
            SaleDetailEntity detail = new SaleDetailEntity();
            detail.setProductId(product.getId());
            detail.setProductName(product.getName());
            detail.setQuantity(entry.getValue());
            detail.setUnitPrice(product.getSalePrice());
            detail.calculateSubtotal();
            sale.addItem(detail);
        }
        sale.recalculateTotals();
        saleRepository.save(sale);

        return confirm(saleId, new ConfirmSaleRequest(request.paymentMethod(), request.amountReceived()));
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private String generateSaleNumber(Long sequenceValue) {
        return OrderNumberGenerator.generate("VLT", sequenceValue);
    }

    private Long getCurrentUserId() {
        return tenantProvider.getUserId();
    }
}


