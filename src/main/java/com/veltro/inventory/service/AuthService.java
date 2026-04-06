package com.veltro.inventory.service;

import com.veltro.inventory.dto.ChangePasswordRequest;
import com.veltro.inventory.dto.LoginRequest;
import com.veltro.inventory.dto.LoginResponse;
import com.veltro.inventory.dto.RefreshRequest;
import com.veltro.inventory.dto.RegisterRequest;
import com.veltro.inventory.dto.WorkerResponse;
import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.BusinessRepository;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.config.JwtProperties;
import com.veltro.inventory.security.CustomUserDetailsService;
import com.veltro.inventory.security.JwtTokenProvider;
import com.veltro.inventory.security.VeltroUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for IAM operations (B1-02).
 *
 * <ul>
 *   <li>{@link #login} — authenticates credentials, issues Access + Refresh tokens.</li>
 *   <li>{@link #refresh} — validates a Refresh token, issues a new Access token.</li>
 *   <li>{@link #logout} — stateless: no server-side action needed; documented for clarity.</li>
 *   <li>{@link #changePassword} — validates current password, hashes and persists the new one.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates username/password and returns a token pair.
     * Delegates credential validation to Spring Security's {@link AuthenticationManager}.
     */
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Long businessId = null;
        if (userDetails instanceof VeltroUserDetails v) {
            businessId = v.getBusinessId();
        }

        log.info("User '{}' logged in successfully (bid={})", request.username(), businessId);

        return LoginResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpiration(),
                request.username(),
                role,
                businessId);
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    /**
     * Validates a Refresh token and issues a new Access token.
     * The Refresh token itself is NOT rotated (stateless strategy).
     */
    public LoginResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.isValidRefreshToken(token)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired.");
        }

        String username = jwtTokenProvider.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Long businessId = null;
        if (userDetails instanceof VeltroUserDetails v) {
            businessId = v.getBusinessId();
        }

        log.debug("Access token refreshed for user '{}'", username);

        return LoginResponse.of(
                newAccessToken,
                token,
                jwtProperties.accessTokenExpiration(),
                username,
                role,
                businessId);
    }

    // -------------------------------------------------------------------------
    // Logout (stateless — documented no-op on the server)
    // -------------------------------------------------------------------------

    /**
     * Stateless logout. The client is responsible for discarding both tokens.
     * This method exists for future extension (e.g., token blocklist) without
     * changing the controller contract.
     */
    public void logout(String username) {
        log.info("User '{}' logged out (stateless — client must discard tokens)", username);
    }

    // -------------------------------------------------------------------------
    // Register (ADMIN only — creates business + admin user)
    // -------------------------------------------------------------------------

    /**
     * Registers a new ADMIN user and creates their business.
     * Only ADMIN role can self-register. Workers are created via {@link #createWorker}.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (request.businessName() == null || request.businessName().isBlank()) {
            throw new IllegalArgumentException("Business name is required for registration");
        }

        if (userRepository.findByEmailAndActiveTrue(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Create the business first (owner set after user creation)
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

    // -------------------------------------------------------------------------
    // Create worker (ADMIN creates CASHIER/WAREHOUSE in their business)
    // -------------------------------------------------------------------------

    /**
     * Creates a worker account (CASHIER or WAREHOUSE) in the admin's business.
     *
     * @param adminBusinessId the businessId of the admin creating the worker
     * @param request         the worker details
     * @return the created UserEntity
     */
    @Transactional
    public UserEntity createWorker(Long adminBusinessId, RegisterRequest request) {
        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid role. Must be CASHIER or WAREHOUSE");
        }
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot create ADMIN workers. Use registration instead.");
        }

        if (userRepository.findByUsernameAndBusinessId(request.username(), adminBusinessId).isPresent()) {
            throw new IllegalArgumentException("Username already exists in this business");
        }

        if (userRepository.findByEmailAndActiveTrue(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        UserEntity worker = new UserEntity();
        worker.setUsername(request.username());
        worker.setEmail(request.email());
        worker.setPasswordHash(passwordEncoder.encode(request.password()));
        worker.setRole(role);
        worker.setBusinessId(adminBusinessId);
        worker.setActive(true);
        worker = userRepository.save(worker);

        log.info("Worker '{}' ({}) created in business {}", worker.getUsername(), role, adminBusinessId);
        return worker;
    }

    // -------------------------------------------------------------------------
    // Change password
    // -------------------------------------------------------------------------

    /**
     * Validates the current password and persists the new BCrypt hash.
     * BCrypt cost factor is defined by the {@link PasswordEncoder} bean (12).
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user '{}'", username);
    }

    // -------------------------------------------------------------------------
    // List workers in a business
    // -------------------------------------------------------------------------

    /**
     * Returns all active users (workers) belonging to the given business.
     *
     * @param businessId the business to list workers for
     * @return list of worker DTOs (excludes passwordHash)
     */
    @Transactional(readOnly = true)
    public List<WorkerResponse> getWorkers(Long businessId) {
        return userRepository.findAllByBusinessIdAndActiveTrue(businessId).stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .map(u -> new WorkerResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.isActive(),
                        u.getCreatedAt()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Deactivate worker (soft delete)
    // -------------------------------------------------------------------------

    /**
     * Deactivates (soft-deletes) a worker in the admin's business.
     * Only non-ADMIN workers can be deactivated.
     *
     * @param workerId    the ID of the worker to deactivate
     * @param businessId  the admin's business ID (for tenant isolation)
     */
    @Transactional
    public void deactivateWorker(Long workerId, Long businessId) {
        UserEntity worker = userRepository.findById(workerId)
                .orElseThrow(() -> new NotFoundException("Worker not found with id: " + workerId));

        if (!worker.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Worker does not belong to your business");
        }
        if (worker.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot deactivate ADMIN users from this endpoint");
        }

        worker.setActive(false);
        userRepository.save(worker);
        log.info("Worker '{}' (id={}) deactivated in business {}", worker.getUsername(), workerId, businessId);
    }

    // -------------------------------------------------------------------------
    // Update worker role
    // -------------------------------------------------------------------------

    /**
     * Updates the role of a worker in the admin's business.
     * Only CASHIER ↔ WAREHOUSE transitions are allowed.
     *
     * @param workerId    the ID of the worker
     * @param newRole     the new role (CASHIER or WAREHOUSE)
     * @param businessId  the admin's business ID (for tenant isolation)
     */
    @Transactional
    public WorkerResponse updateWorkerRole(Long workerId, String newRole, Long businessId) {
        Role role;
        try {
            role = Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be CASHIER or WAREHOUSE");
        }
        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot assign ADMIN role to workers");
        }

        UserEntity worker = userRepository.findById(workerId)
                .orElseThrow(() -> new NotFoundException("Worker not found with id: " + workerId));

        if (!worker.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Worker does not belong to your business");
        }
        if (worker.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot change role of ADMIN users");
        }

        worker.setRole(role);
        userRepository.save(worker);
        log.info("Worker '{}' (id={}) role updated to {} in business {}", worker.getUsername(), workerId, role, businessId);

        return new WorkerResponse(
                worker.getId(),
                worker.getUsername(),
                worker.getEmail(),
                worker.getRole().name(),
                worker.isActive(),
                worker.getCreatedAt());
    }
}
