package com.veltro.inventory.security;

import java.util.Optional;

/**
 * Injectable gateway for authenticated tenant information.
 */
public interface TenantProvider {

    Long getBusinessId();

    Long getUserId();

    String getUsername();

    Optional<String> getOptionalUsername();
}
