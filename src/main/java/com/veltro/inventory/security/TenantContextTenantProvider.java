package com.veltro.inventory.security;

import org.springframework.stereotype.Component;

/**
 * Spring implementation of {@link TenantProvider} backed by {@link TenantContext}.
 */
@Component
public class TenantContextTenantProvider implements TenantProvider {

    @Override
    public Long getBusinessId() {
        return TenantContext.getBusinessId();
    }

    @Override
    public Long getUserId() {
        return TenantContext.getUserId();
    }

    @Override
    public String getUsername() {
        return TenantContext.getUsername();
    }
}
