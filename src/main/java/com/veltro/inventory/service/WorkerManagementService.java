package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerCreatedResponse;
import com.veltro.inventory.dto.auth.WorkerResponse;
import com.veltro.inventory.model.Role;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Service dedicated to managing worker accounts (CASHIER/WAREHOUSE) (B1-02).
 *
 * <p>Extracted from {@code AuthService} to separate worker lifecycle management
 * from authentication and registration concerns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a worker account (CASHIER or WAREHOUSE) in the admin's business.
     */
    @Transactional
    public WorkerCreatedResponse createWorker(Long adminBusinessId, RegisterRequest request) {
        Role role = parseWorkerRole(request.role());
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
        return new WorkerCreatedResponse(
                worker.getId(),
                worker.getUsername(),
                worker.getEmail(),
                worker.getRole().name(),
                worker.getCreatedAt());
    }

    /**
     * Returns all active users (workers) belonging to the given business.
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

    /**
     * Returns the total count of active workers belonging to the given business.
     */
    @Transactional(readOnly = true)
    public long getWorkerCount(Long businessId) {
        return userRepository.countByBusinessIdAndActiveTrueAndRoleNot(businessId, Role.ADMIN);
    }

    /**
     * Deactivates (soft-deletes) a worker in the admin's business.
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

    /**
     * Updates the role of a worker in the admin's business.
     */
    @Transactional
    public WorkerResponse updateWorkerRole(Long workerId, String newRole, Long businessId) {
        Role role = parseWorkerRole(newRole);
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

    public Role parseWorkerRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new IllegalArgumentException("Invalid role. Must be CASHIER or WAREHOUSE");
        }

        String normalized = normalizeRole(rawRole);

        return switch (normalized) {
            case "CASHIER", "CAJERO" -> Role.CASHIER;
            case "WAREHOUSE", "ALMACEN", "BODEGA" -> Role.WAREHOUSE;
            case "ADMIN", "ADMINISTRADOR" -> Role.ADMIN;
            default -> throw new IllegalArgumentException("Invalid role. Must be CASHIER or WAREHOUSE");
        };
    }

    private String normalizeRole(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
