package com.veltro.inventory.controller;

import com.veltro.inventory.dto.inventory.InventoryMovementResponse;
import com.veltro.inventory.dto.inventory.InventoryResponse;
import com.veltro.inventory.dto.inventory.StockAdjustmentRequest;
import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.dto.inventory.StockExitRequest;
import com.veltro.inventory.dto.inventory.UpdateStockLimitsRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for inventory management (B1-04).
 *
 * Role rules (RF-12):
 * <ul>
 *   <li>GET endpoints: ADMIN, WAREHOUSE, CASHIER.</li>
 *   <li>POST / PUT (mutations): ADMIN or WAREHOUSE only.</li>
 * </ul>
 *
 * All endpoints are keyed on {@code productId} 窶・the natural key for inventory
 * from the caller's perspective (1-to-1 with products).
 *
 * AC-04: {@code POST /exit} will return HTTP 422 with error code
 * "INSUFFICIENT_STOCK" if the requested quantity exceeds the current stock.
 *
 * AC-07: {@code GET /movements} returns a {@link PageResponse} with pagination metadata.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // -------------------------------------------------------------------------
    // GET endpoints 窶・readable by all authenticated roles
    // -------------------------------------------------------------------------

    /**
     * Returns all inventory records with pagination and optional name search (B11).
     */
    @GetMapping
    public ResponseEntity<PageResponse<InventoryResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(inventoryService.findAll(search, pageable));
    }

    /**
     * Returns the current stock record for the given product.
     * Returns 404 when the product has no inventory row.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }

    /**
     * Paginated movement history for the given product's inventory (AC-07).
     * Default: page=0, size=20, newest-first.
     */
    @GetMapping("/{productId}/movements")
    public ResponseEntity<PageResponse<InventoryMovementResponse>> getMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getMovements(productId, pageable));
    }

    // -------------------------------------------------------------------------
    // POST / PUT endpoints 窶・ADMIN or WAREHOUSE only
    // -------------------------------------------------------------------------

    /**
     * Records a stock entry (goods received, manual addition).
     * Increases {@code currentStock} by the given quantity.
     */
    @PostMapping("/{productId}/entry")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<InventoryResponse> recordEntry(
            @PathVariable Long productId,
            @Valid @RequestBody StockEntryRequest request) {
        return ResponseEntity.ok(inventoryService.recordEntry(productId, request));
    }

    /**
     * Records a stock exit (shrinkage, manual removal).
     * Returns HTTP 422 with "INSUFFICIENT_STOCK" if quantity > currentStock (AC-04).
     */
    @PostMapping("/{productId}/exit")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<InventoryResponse> recordExit(
            @PathVariable Long productId,
            @Valid @RequestBody StockExitRequest request) {
        return ResponseEntity.ok(inventoryService.recordExit(productId, request));
    }

    /**
     * Sets stock to a specific absolute value (physical count correction).
     * A non-blank reason is required to maintain an auditable trail.
     */
    @PostMapping("/{productId}/adjustment")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<InventoryResponse> recordAdjustment(
            @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.recordAdjustment(productId, request));
    }

    /**
     * Updates the min/max stock alert thresholds for the given product.
     */
    @PutMapping("/{productId}/limits")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<InventoryResponse> updateLimits(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockLimitsRequest request) {
        return ResponseEntity.ok(inventoryService.updateLimits(productId, request));
    }
}

