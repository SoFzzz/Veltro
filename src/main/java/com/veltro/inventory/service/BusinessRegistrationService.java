package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.BusinessRepository;
import com.veltro.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to business and initial admin user registration.
 *
 * <p>Extracted from {@code AuthService} to separate business creation logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessRegistrationService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new ADMIN user and creates their business.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (request.businessName() == null || request.businessName().isBlank()) {
            throw new IllegalArgumentException("Business name is required for registration");
        }

        if (userRepository.findByEmailAndActiveTrue(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Create the business first
        BusinessEntity business = new BusinessEntity();
        business.setName(request.businessName().trim());
        business.setActive(true);
        business = businessRepository.save(business);

        // Create the ADMIN user linked to this business
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        user.setBusinessId(business.getId());
        user.setActive(true);
        user = userRepository.save(user);

        // Set owner on business
        business.setOwner(user);
        businessRepository.save(business);

        log.info("Admin '{}' registered with business '{}' (bid={})",
                request.username(), business.getName(), business.getId());
    }
}
