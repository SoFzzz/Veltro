package com.veltro.inventory.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class to extract multi-tenant information from the SecurityContext.
 *
 * All service-layer code should use this instead of directly accessing
 * SecurityContextHolder. The principal is expected to be a {@link VeltroUserDetails}
 * which carries userId and businessId.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> MANUAL_BUSINESS_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> MANUAL_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> MANUAL_USERNAME = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    /**
     * Returns the businessId of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user or principal is not VeltroUserDetails
     */
    public static Long getBusinessId() {
        Long override = MANUAL_BUSINESS_ID.get();
        if (override != null) {
            return override;
        }
        return getPrincipal().getBusinessId();
    }

    /**
     * Returns the userId of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user or principal is not VeltroUserDetails
     */
    public static Long getUserId() {
        Long override = MANUAL_USER_ID.get();
        if (override != null) {
            return override;
        }
        return getPrincipal().getUserId();
    }

    /**
     * Returns the username of the currently authenticated user.
     *
     * @throws IllegalStateException if no authenticated user
     */
    public static String getUsername() {
        String override = MANUAL_USERNAME.get();
        if (override != null) {
            return override;
        }
        return getPrincipal().getUsername();
    }

    /**
     * Returns the current username when available, without throwing on anonymous
     * or missing authentication contexts.
     */
    public static Optional<String> getOptionalUsername() {
        String override = MANUAL_USERNAME.get();
        if (override != null) {
            return Optional.of(override);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(auth.getName());
        }
        return Optional.empty();
    }

    /**
     * Sets manual override for background or async threads without SecurityContext.
     * MUST be used within a try-finally block to prevent ThreadLocal leaks.
     *
     * <p>Usage:
     * <pre>{@code
     * TenantContext.setOverride(businessId, userId, username);
     * try {
     *     // background/async logic
     * } finally {
     *     TenantContext.clearOverride();
     * }
     * }</pre>
     */
    public static void setOverride(Long businessId, Long userId, String username) {
        MANUAL_BUSINESS_ID.set(businessId);
        MANUAL_USER_ID.set(userId);
        MANUAL_USERNAME.set(username);
    }

    /**
     * Clears the manual override. Must ALWAYS be called in a finally block
     * after {@link #setOverride(Long, Long, String)} to prevent ThreadLocal memory leaks and
     * cross-tenant data corruption in pooled thread environments.
     */
    public static void clearOverride() {
        MANUAL_BUSINESS_ID.remove();
        MANUAL_USER_ID.remove();
        MANUAL_USERNAME.remove();
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
