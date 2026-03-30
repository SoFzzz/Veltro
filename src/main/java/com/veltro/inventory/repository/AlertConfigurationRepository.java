package com.veltro.inventory.repository;

import com.veltro.inventory.model.AlertConfigurationEntity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertConfigurationRepository
        extends JpaRepository<AlertConfigurationEntity, Long>, com.veltro.inventory.domain.inventory.ports.AlertConfigurationRepository {

    @Override
    Optional<AlertConfigurationEntity> findByProductIdAndActiveTrue(Long productId);
}
