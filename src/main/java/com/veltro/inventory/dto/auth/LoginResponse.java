package com.veltro.inventory.dto.auth;

/**
 * Response payload for successful authentication.
 * Returned by {@code POST /api/v1/auth/login} and {@code POST /api/v1/auth/refresh}.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        String role,
        Long businessId,
        String email,
        String businessName,
        String adminName
) {
    /**
     * Convenience factory. {@code tokenType} is always "Bearer".
     */
    public static LoginResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String username,
            String role,
            Long businessId) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role, businessId, null, null, null);
    }

    public static LoginResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String username,
            String role,
            Long businessId,
            String email,
            String businessName,
            String adminName) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role, businessId, email, businessName, adminName);
    }
}

