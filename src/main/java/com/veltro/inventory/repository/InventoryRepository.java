package com.veltro.inventory.repository;

import com.veltro.inventory.model.InventoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    /**
     * Finds inventory by product ID, filtering by both inventory and product active status.
     * BUG-09 fix: excludes inventory for inactive products.
     */
    @Query("SELECT i FROM InventoryEntity i " +
           "WHERE i.product.id = :productId " +
           "AND i.active = true " +
           "AND i.product.active = true " +
           "AND i.businessId = :businessId")
    @EntityGraph(attributePaths = {"product"})
    Optional<InventoryEntity> findByProductIdAndActiveTrueAndBusinessId(
            @Param("productId") Long productId, 
            @Param("businessId") Long businessId);

    /**
     * Finds all active inventory entries for active products,
     * optionally filtering by product name (case-insensitive substring).
     * B11 fix: enables the search parameter from the inventory UI.
     */
    @Query("SELECT i FROM InventoryEntity i " +
           "WHERE i.active = true " +
           "AND i.product.active = true " +
           "AND i.businessId = :businessId " +
           "AND (:search IS NULL OR LOWER(i.product.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    @EntityGraph(attributePaths = {"product"})
    Page<InventoryEntity> findAllByActiveTrueAndBusinessIdAndSearch(
            @Param("businessId") Long businessId,
            @Param("search") String search,
            Pageable pageable);
}
