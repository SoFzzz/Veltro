package com.veltro.inventory.repository;

import com.veltro.inventory.model.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByParentCategoryIsNullAndActiveTrueAndBusinessId(Long businessId);

    Optional<CategoryEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    /**
     * Finds a category by name and business, regardless of active status.
     * Used to detect duplicates including soft-deleted categories (BUG-07).
     */
    Optional<CategoryEntity> findByNameAndBusinessId(String name, Long businessId);

    /**
     * Finds a category by ID and business, regardless of active status.
     * Used for reactivation (BUG-14).
     */
    Optional<CategoryEntity> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByIdAndBusinessId(Long id, Long businessId);
}
