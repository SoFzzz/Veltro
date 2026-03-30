package com.veltro.inventory.domain.iam.ports;

import com.veltro.inventory.model.UserEntity;

import java.util.Optional;

/**
 * Domain port (output) for user persistence.
 *
 * This interface belongs to the domain layer and has NO dependency on Spring or
 * any infrastructure framework. The infrastructure adapter
 * ({@code UserJpaRepository}) implements this contract.
 */
public interface UserRepository {

    Optional<UserEntity> findByUsernameAndActiveTrue(String username);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    Optional<UserEntity> findById(Long id);

    Optional<UserEntity> findByUsernameAndBusinessId(String username, Long businessId);

    UserEntity save(UserEntity user);
}
