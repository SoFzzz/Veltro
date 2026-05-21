package com.veltro.inventory.repository;

import com.veltro.inventory.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndActiveTrue(String username);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    Optional<UserEntity> findByUsernameAndBusinessId(String username, Long businessId);

    boolean existsByUsernameAndBusinessId(String username, Long businessId);

    boolean existsByEmail(String email);

    /**
     * Lists all active users belonging to a given business.
     * Used by the Workers management page to display employees.
     */
    List<UserEntity> findAllByBusinessIdAndActiveTrue(Long businessId);

    long countByBusinessIdAndActiveTrueAndRoleNot(Long businessId, com.veltro.inventory.model.Role role);
}