package com.veltro.inventory.controller;

import com.veltro.inventory.dto.catalog.CreateProductRequest;
import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.catalog.UpdateProductRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.exception.DuplicateProductConflictException;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * REST controller for product catalog management (B1-03).
 *
 * Role rules (RF-12):
 * <ul>
 *   <li>GET /products and GET /products/{id}: ADMIN, WAREHOUSE, CASHIER.</li>
 *   <li>GET /products/barcode/{barcode}: any authenticated user (primary POS path — UC-01).</li>
 *   <li>POST / PUT: ADMIN or WAREHOUSE only.</li>
 * </ul>
 *
 * AC-07: {@code GET /products} returns a {@link PageResponse} with pagination metadata.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // -------------------------------------------------------------------------
    // GET endpoints
    // -------------------------------------------------------------------------

    /**
     * Paginated product listing (AC-07).
     * Defaults: page=0, size=20, sort=id,asc.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /**
     * Barcode lookup — the primary endpoint called by the POS scanner (UC-01).
     * Returns 404 when no active product matches the barcode.
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ProductResponse> findByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.findByBarcode(barcode));
    }

    /**
     * Paginated listing of inactive (soft-deleted) products.
     * Restricted to ADMIN and WAREHOUSE roles per RF-12.
     */
    @GetMapping("/inactive")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<PageResponse<ProductResponse>> listInactiveProducts(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.findAllInactive(pageable));
    }
    // -------------------------------------------------------------------------
    // POST / PUT endpoints — ADMIN or WAREHOUSE only
    // -------------------------------------------------------------------------

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<?> create(
            @Valid @org.springframework.web.bind.annotation.RequestPart("product") CreateProductRequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) MultipartFile image) {
        
        ProductResponse response;
        try {
            response = productService.create(request);
        } catch (DuplicateResourceException | InactiveResourceExistsException e) {
            Long existingId = null;
            if (e instanceof InactiveResourceExistsException inactiveResource) {
                existingId = inactiveResource.getExistingResourceId();
            } else {
                try {
                    existingId = productService.findByBarcode(request.barcode()).id();
                } catch (Exception ignore) { }
            }
            throw new DuplicateProductConflictException(existingId, e);
        }
        
        if (image != null && !image.isEmpty()) {
            try {
                productService.uploadImages(response.id(), java.util.List.of(image));
            } catch (Exception e) {
                log.warn("Image upload failed for product {}: {}", response.id(), e.getMessage());
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<Void> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") java.util.List<MultipartFile> images) {
        productService.uploadImages(id, images);
        return ResponseEntity.ok().build();
    }

    /**
     * Soft-deletes a product (AC-05). Sets {@code active=false}; the record
     * is preserved in the database for audit, sales history, and purchase orders.
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a soft-deleted product (BUG-14 fix).
     * Sets {@code active=true} so the product appears in listings again.
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ProductResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(productService.reactivate(id));
    }

    /**
     * Hard-deletes a product.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        productService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}

