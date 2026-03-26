package com.veltro.inventory.infrastructure.adapters.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Extended {@link User} that carries multi-tenant context (userId + businessId).
 *
 * These extra fields are embedded into the SecurityContext principal so that
 * any service layer code can extract them via {@link TenantContext} without
 * an additional DB query.
 */
public class VeltroUserDetails extends User {

    private final Long userId;
    private final Long businessId;

    public VeltroUserDetails(String username, String password,
                             Collection<? extends GrantedAuthority> authorities,
                             Long userId, Long businessId) {
        super(username, password, authorities);
        this.userId = userId;
        this.businessId = businessId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBusinessId() {
        return businessId;
    }
}
