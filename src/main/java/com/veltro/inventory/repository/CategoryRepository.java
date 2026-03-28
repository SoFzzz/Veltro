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
}
