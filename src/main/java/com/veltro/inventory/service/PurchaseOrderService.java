package com.veltro.inventory.service;

import com.veltro.inventory.dto.purchasing.AddOrderItemRequest;
import com.veltro.inventory.dto.purchasing.CreatePurchaseOrderRequest;
import com.veltro.inventory.dto.purchasing.PurchaseOrderResponse;
import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.event.ReceivedItemInfo;
import com.veltro.inventory.mapper.PurchaseOrderMapper;
import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.repository.PurchaseOrderRepository;
import com.veltro.inventory.repository.SupplierRepository;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for purchase order management (B2-04).
 *
 * <p>Manages the complete purchase order lifecycle with State Pattern integration,
 * Prototype Pattern for order cloning, and event publishing for inventory updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PurchaseOrderMapper orderMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditCommandExecutor auditCommandExecutor;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Finds all active purchase orders, optionally filtered by status.
     *
     * @param status optional status filter (null returns all)
     * @return list of purchase order responses
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findAll(PurchaseOrderStatus status) {
        Long businessId = TenantContext.getBusinessId();

        if (status != null) {
            return orderRepository.findAllByActiveTrueAndStatusAndBusinessIdOrderByIdAsc(status, businessId)
                    .stream()
                    .map(orderMapper::toResponse)
                    .toList();
        }

        return orderRepository.findAllByActiveTrueAndBusinessIdOrderByIdAsc(businessId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    /**
     * Finds all active purchase orders for a supplier.
     *
     * @param supplierId supplier ID
     * @return list of purchase order responses
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> findBySupplier(Long supplierId) {
        Long businessId = TenantContext.getBusinessId();
        return orderRepository.findBySupplierIdAndActiveTrueAndBusinessIdOrderByIdAsc(supplierId, businessId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    /**
     * Finds a purchase order by ID.
     *
     * @param orderId order ID
     * @return purchase order response
     * @throws NotFoundException if order not found or inactive
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse findById(Long orderId) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByIdAndActiveTrueAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with id: " + orderId));
        
        return orderMapper.toResponse(order);
    }

    /**
     * Finds a purchase order by order number.
     *
     * @param orderNumber order number
     * @return purchase order response
     * @throws NotFoundException if order not found or inactive
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse findByOrderNumber(String orderNumber) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByOrderNumberAndActiveTrueAndBusinessId(orderNumber, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with order number: " + orderNumber));
        
        return orderMapper.toResponse(order);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    /**
     * Creates a new purchase order in PENDING status.
     *
     * @param request create request
     * @return created purchase order response
     * @throws NotFoundException if supplier not found
     */
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        Long businessId = TenantContext.getBusinessId();
        SupplierEntity supplier = supplierRepository.findByIdAndActiveTrueAndBusinessId(request.supplierId(), businessId)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + request.supplierId()));

        UserEntity currentUser = getCurrentUser();

        Long sequenceValue = orderRepository.getNextOrderSequenceValue();
        String orderNumber = generateOrderNumber(sequenceValue);

        PurchaseOrderEntity order = orderMapper.toEntity(request);
        order.setBusinessId(businessId);
        order.setOrderNumber(orderNumber);
        order.setStatus(PurchaseOrderStatus.PENDING);
        order.setSupplier(supplier);
        order.setTotal(BigDecimal.ZERO);
        order.setRequestedBy(currentUser);

        PurchaseOrderEntity saved = orderRepository.save(order);
        log.info("Created purchase order: {} for supplier: {}", orderNumber, supplier.getCompanyName());
        
        return orderMapper.toResponse(saved);
    }

    /**
     * Adds an item to a purchase order (State Pattern delegation).
     *
     * @param orderId order ID
     * @param request item request
     * @return updated purchase order response
     * @throws NotFoundException if order or product not found
     */
    @Transactional
    public PurchaseOrderResponse addItem(Long orderId, AddOrderItemRequest request) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByIdAndActiveTrueAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with id: " + orderId));

        ProductEntity product = productRepository.findByIdAndActiveTrueAndBusinessId(request.productId(), businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.productId()));

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setProduct(product);
        detail.setRequestedQuantity(request.requestedQuantity());
        detail.setUnitCost(request.unitCost());

        // State Pattern delegation
        order.addItem(detail);
        
        PurchaseOrderEntity updated = orderRepository.save(order);
        log.info("Added item {} (qty: {}) to order {}", product.getName(), request.requestedQuantity(), order.getOrderNumber());
        
        return orderMapper.toResponse(updated);
    }

    /**
     * Removes an item from a purchase order (soft delete - State Pattern delegation).
     *
     * @param orderId order ID
     * @param detailId detail ID to remove
     * @return updated purchase order response
     * @throws NotFoundException if order not found
     */
    @Transactional
    public PurchaseOrderResponse removeItem(Long orderId, Long detailId) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByIdAndActiveTrueAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with id: " + orderId));

        // State Pattern delegation
        order.removeItem(detailId);
        
        PurchaseOrderEntity updated = orderRepository.save(order);
        log.info("Removed item {} from order {}", detailId, order.getOrderNumber());
        
        return orderMapper.toResponse(updated);
    }

    /**
     * Voids a purchase order (State Pattern delegation).
     *
     * <p>Creates forensic audit record (B3-03) capturing before/after state.
     *
     * @param orderId order ID
     * @return voided purchase order response
     * @throws NotFoundException if order not found
     */
    @Transactional
    public PurchaseOrderResponse voidOrder(Long orderId) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByIdAndActiveTrueAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with id: " + orderId));

        // Capture state BEFORE voiding for audit (B3-03)
        final Map<String, Object> beforeSnapshot = buildOrderSnapshot(order);

        // State Pattern delegation
        order.voidOrder();
        
        PurchaseOrderEntity updated = orderRepository.save(order);

        // Create forensic audit record (B3-03)
        auditCommandExecutor.execute(
                AuditEntityType.PURCHASE_ORDER,
                updated.getId(),
                AuditAction.VOID,
                () -> beforeSnapshot,
                () -> updated,
                (result) -> buildOrderSnapshot(updated),
                RequestAuditContext.empty()
        );

        log.info("Voided purchase order: {}", order.getOrderNumber());
        
        return orderMapper.toResponse(updated);
    }

    /**
     * Marks an entire order as received and publishes OrderReceivedEvent.
     *
     * <p>Creates forensic audit record (B3-03) capturing before/after state.
     *
     * @param orderId order ID
     * @return updated purchase order response
     * @throws NotFoundException if order not found
     */
    @Transactional
    public PurchaseOrderResponse markAsReceived(Long orderId) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity order = orderRepository.findByIdAndActiveTrueAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found with id: " + orderId));

        // Capture state BEFORE receiving for audit (B3-03)
        final Map<String, Object> beforeSnapshot = buildOrderSnapshot(order);

        // Mark all items as fully received
        List<PurchaseOrderDetailEntity> activeDetails = order.getDetails().stream()
                .filter(d -> d.isActive())
                .collect(Collectors.toList());

        for (PurchaseOrderDetailEntity detail : activeDetails) {
            detail.setReceivedQuantity(detail.getRequestedQuantity());
        }

        // State transition to RECEIVED
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        PurchaseOrderEntity updated = orderRepository.save(order);

        // Publish event for inventory increment
        publishOrderReceivedEvent(updated, activeDetails);

        // Create forensic audit record (B3-03)
        auditCommandExecutor.execute(
                AuditEntityType.PURCHASE_ORDER,
                updated.getId(),
                AuditAction.RECEIVE,
                () -> beforeSnapshot,
                () -> updated,
                (result) -> buildOrderSnapshot(updated),
                RequestAuditContext.empty()
        );
        
        log.info("Marked purchase order {} as fully received", order.getOrderNumber());
        return orderMapper.toResponse(updated);
    }

    /**
     * Creates a clone of an existing order using the Prototype Pattern.
     *
     * @param sourceOrderId source order ID to clone
     * @return cloned purchase order response
     * @throws NotFoundException if source order not found
     */
    @Transactional
    public PurchaseOrderResponse cloneOrder(Long sourceOrderId) {
        Long businessId = TenantContext.getBusinessId();
        PurchaseOrderEntity sourceOrder = orderRepository.findByIdAndActiveTrueAndBusinessId(sourceOrderId, businessId)
                .orElseThrow(() -> new NotFoundException("Source purchase order not found with id: " + sourceOrderId));

        UserEntity currentUser = getCurrentUser();

        // Prototype Pattern
        PurchaseOrderEntity clonedOrder = sourceOrder.cloneForNewOrder();
        clonedOrder.setBusinessId(businessId);
        clonedOrder.setRequestedBy(currentUser);
        
        // Generate new order number
        Long sequenceValue = orderRepository.getNextOrderSequenceValue();
        String newOrderNumber = generateOrderNumber(sequenceValue);
        clonedOrder.setOrderNumber(newOrderNumber);

        PurchaseOrderEntity saved = orderRepository.save(clonedOrder);
        log.info("Cloned purchase order {} from source order {}", newOrderNumber, sourceOrder.getOrderNumber());
        
        return orderMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Private Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves the current authenticated user from TenantContext.
     *
     * @return the UserEntity of the currently authenticated user
     * @throws NotFoundException if the user cannot be found in the database
     */
    private UserEntity getCurrentUser() {
        String username = TenantContext.getUsername();
        return userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotFoundException("Current user not found: " + username));
    }

    /**
     * Generates order number in format PO-YYYY-NNNNNN.
     */
    private String generateOrderNumber(Long sequenceValue) {
        int currentYear = LocalDateTime.now().getYear();
        return String.format("PO-%d-%06d", currentYear, sequenceValue);
    }

    /**
     * Publishes OrderReceivedEvent for inventory increment.
     */
    private void publishOrderReceivedEvent(PurchaseOrderEntity order, List<PurchaseOrderDetailEntity> receivedDetails) {
        List<ReceivedItemInfo> receivedItems = receivedDetails.stream()
                .map(detail -> new ReceivedItemInfo(
                        detail.getProduct().getId(),
                        detail.getProduct().getName(),
                        detail.getReceivedQuantity(),
                        detail.getUnitCost(),
                        detail.getUnitCost().multiply(BigDecimal.valueOf(detail.getReceivedQuantity()))
                ))
                .collect(Collectors.toList());

        OrderReceivedEvent event = new OrderReceivedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getCompanyName(),
                order.getTotal(),
                LocalDateTime.now(),
                SecurityContextHolder.getContext().getAuthentication() != null
                        ? SecurityContextHolder.getContext().getAuthentication().getName() : "System",
                receivedItems
        );

        applicationEventPublisher.publishEvent(event);
        log.info("Published OrderReceivedEvent for order: {}", order.getOrderNumber());
    }

    /**
     * Builds a snapshot map of purchase order state for forensic audit (B3-03).
     *
     * @param order the purchase order entity to snapshot
     * @return map containing order state for audit record
     */
    private Map<String, Object> buildOrderSnapshot(PurchaseOrderEntity order) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", order.getId());
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        snapshot.put("supplierId", order.getSupplier() != null ? order.getSupplier().getId() : null);
        snapshot.put("supplierName", order.getSupplier() != null ? order.getSupplier().getCompanyName() : null);
        snapshot.put("total", order.getTotal());
        snapshot.put("notes", order.getNotes());

        // Capture active details
        List<Map<String, Object>> details = order.getDetails().stream()
                .filter(PurchaseOrderDetailEntity::isActive)
                .map(d -> {
                    Map<String, Object> detailMap = new LinkedHashMap<>();
                    detailMap.put("id", d.getId());
                    detailMap.put("productId", d.getProduct() != null ? d.getProduct().getId() : null);
                    detailMap.put("productName", d.getProduct() != null ? d.getProduct().getName() : null);
                    detailMap.put("requestedQuantity", d.getRequestedQuantity());
                    detailMap.put("receivedQuantity", d.getReceivedQuantity());
                    detailMap.put("unitCost", d.getUnitCost());
                    return detailMap;
                })
                .collect(Collectors.toList());
        snapshot.put("details", details);

        return snapshot;
    }
}


