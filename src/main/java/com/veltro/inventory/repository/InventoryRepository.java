package com.veltro.inventory.repository;

import com.veltro.inventory.model.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementing the {@link com.veltro.inventory.domain.inventory.ports.InventoryRepository} output port.
 *
 * Spring Data JPA derives all declared query methods from method names.
 * The {@code findByProductIdAndActiveTrue} method uses the UNIQUE index on
 * {@code inventory.product_id} created in V2 migration — effectively an
 * index scan on every lookup.
 */
@Repository
public interface InventoryRepository
        extends JpaRepository<InventoryEntity, Long>, com.veltro.inventory.domain.inventory.ports.InventoryRepository {
}
