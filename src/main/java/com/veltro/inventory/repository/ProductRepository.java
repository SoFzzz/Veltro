package com.veltro.inventory.repository;

import com.veltro.inventory.model.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findAllByActiveTrueAndBusinessId(Long businessId, Pageable pageable);

    Optional<ProductEntity> findByBarcodeAndActiveTrueAndBusinessId(String barcode, Long businessId);

    Optional<ProductEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    /**
     * Finds a product by barcode and business, regardless of active status.
     * Used to detect duplicates including soft-deleted products (BUG-15).
     */
    Optional<ProductEntity> findByBarcodeAndBusinessId(String barcode, Long businessId);

    /**
     * Finds a product by SKU and business, regardless of active status.
     * Used to detect duplicates including soft-deleted products (BUG-15).
     */
    Optional<ProductEntity> findBySkuAndBusinessId(String sku, Long businessId);

    /**
     * Finds a product by ID and business, regardless of active status.
     * Used for reactivation (BUG-14).
     */
    Optional<ProductEntity> findByIdAndBusinessId(Long id, Long businessId);
}
