package com.veltro.inventory.repository;

import com.veltro.inventory.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndActiveTrue(String username);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    Optional<UserEntity> findByUsernameAndBusinessId(String username, Long businessId);

    boolean existsByUsernameAndBusinessId(String username, Long businessId);

    boolean existsByEmail(String email);
}