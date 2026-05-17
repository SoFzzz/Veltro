package com.veltro.inventory.config;

import com.veltro.inventory.security.TenantContext;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Supplies the current auditor username to Spring Data JPA auditing.
 *
 * Resolution order:
 *   1. Authenticated, non-anonymous principal → use principal name.
 *   2. No security context or anonymous → fall back to "SYSTEM" so that
 *      batch jobs and bootstrap data are still properly attributed (ADR-004).
 */
public class VeltroAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_USER = "SYSTEM";

    @Override
    public Optional<String> getCurrentAuditor() {
        return TenantContext.getOptionalUsername()
                .or(() -> Optional.of(SYSTEM_USER));
    }
}
