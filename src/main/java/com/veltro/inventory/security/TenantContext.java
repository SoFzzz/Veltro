package com.veltro.inventory.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract multi-tenant information from the SecurityContext.
 *
 * All service-layer code should use this instead of directly accessing
 * SecurityContextHolder. The principal is expected to be a {@link VeltroUserDetails}
 * which carries userId and businessId.
 */
public final class TenantContext {

    private TenantContext() {
        // utility class
    }

    /**
     * Returns the businessId of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user or principal is not VeltroUserDetails
     */
    public static Long getBusinessId() {
        return getPrincipal().getBusinessId();
    }

    /**
     * Returns the userId of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user or principal is not VeltroUserDetails
     */
    public static Long getUserId() {
        return getPrincipal().getUserId();
    }

    /**
     * Returns the username of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user
     */
    public static String getUsername() {
        return getPrincipal().getUsername();
    }

    private static VeltroUserDetails getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof VeltroUserDetails)) {
            throw new IllegalStateException(
                    "No authenticated VeltroUserDetails found in SecurityContext");
        }
        return (VeltroUserDetails) auth.getPrincipal();
    }
}
