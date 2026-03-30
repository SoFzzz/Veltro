package com.veltro.inventory.repository;

import com.veltro.inventory.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA adapter for {@link com.veltro.inventory.domain.catalog.ports.ProductRepository}.
 *
 * {@code findByBarcodeAndActiveTrue} uses the B-Tree index created in
 * V1 migration ({@code idx_products_barcode}) for O(log n) lookups.
 */
@Repository
public interface ProductRepository
        extends JpaRepository<ProductEntity, Long>, com.veltro.inventory.domain.catalog.ports.ProductRepository {
}
