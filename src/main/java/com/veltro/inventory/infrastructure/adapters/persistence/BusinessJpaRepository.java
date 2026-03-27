package com.veltro.inventory.infrastructure.adapters.persistence;

import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.domain.iam.ports.BusinessRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA adapter for the {@link BusinessRepository} domain port.
 */
@Repository
public interface BusinessJpaRepository extends JpaRepository<BusinessEntity, Long>, BusinessRepository {
}
