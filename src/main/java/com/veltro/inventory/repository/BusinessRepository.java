package com.veltro.inventory.repository;

import com.veltro.inventory.model.BusinessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA adapter for the {@link com.veltro.inventory.domain.iam.ports.BusinessRepository} domain port.
 */
@Repository
public interface BusinessRepository extends JpaRepository<BusinessEntity, Long>, com.veltro.inventory.domain.iam.ports.BusinessRepository {
}
