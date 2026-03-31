package com.veltro.inventory.controller;

import com.veltro.inventory.dto.CategoryResponse;
import com.veltro.inventory.dto.CreateCategoryRequest;
import com.veltro.inventory.dto.UpdateCategoryRequest;
import com.veltro.inventory.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for category management (B1-03).
 *
 * Role rules (RF-12):
 * <ul>
 *   <li>GET endpoints: any authenticated user (ADMIN, WAREHOUSE, CASHIER).</li>
 *   <li>POST / PUT: ADMIN or WAREHOUSE only.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // -------------------------------------------------------------------------
    // GET endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns all root categories with their full subcategory tree.
     * Accessible by any authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listRoots() {
        return ResponseEntity.ok(categoryService.findRoots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    // -------------------------------------------------------------------------
    // POST / PUT endpoints — ADMIN or WAREHOUSE only
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        categoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a soft-deleted category (BUG-14 fix).
     * Sets {@code active=true} so the category appears in listings again.
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<CategoryResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.reactivate(id));
    }

    /**
     * Hard-deletes a category.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        categoryService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
