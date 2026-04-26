package com.veltro.inventory.repository;

import com.veltro.inventory.model.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetailEntity, Long> {
    
    /**
     * Checks if a product has any associated sale detail records.
     * Used to prevent hard-delete of products with sale history.
     */
    @Query("SELECT COUNT(sd) > 0 FROM SaleDetailEntity sd WHERE sd.productId = :productId AND sd.active = true")
    boolean existsByProductIdAndActiveTrue(@Param("productId") Long productId);
}
