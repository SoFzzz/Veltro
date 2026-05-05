package com.veltro.inventory.security;

/**
 * Injectable gateway for authenticated tenant information.
 */
public interface TenantProvider {

    Long getBusinessId();

    Long getUserId();

    String getUsername();
}
