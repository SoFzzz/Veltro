package com.veltro.inventory.repository;

import com.veltro.inventory.model.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link SaleDetailEntity} (B2-01).
 *
 * <p>Extends the domain {@link com.veltro.inventory.domain.pos.ports.SaleDetailRepository} port.
 * <b>NOTE:</b> This repository does NOT have a delete method — soft delete (AC-05)
 * is enforced via {@code active=false} in the domain layer.
 */
@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetailEntity, Long>, com.veltro.inventory.domain.pos.ports.SaleDetailRepository {
    // No additional methods needed — port interface provides all required queries
}
