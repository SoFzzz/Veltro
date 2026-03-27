package com.veltro.inventory.repository;

import com.veltro.inventory.model.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA adapter for {@link com.veltro.inventory.domain.catalog.ports.CategoryRepository}.
 *
 * Method names are derived from the port interface — Spring Data generates
 * the queries automatically. No additional annotations required.
 */
@Repository
public interface CategoryRepository
        extends JpaRepository<CategoryEntity, Long>, com.veltro.inventory.domain.catalog.ports.CategoryRepository {
}
