package com.veltro.inventory.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/v1/auth/change-password}.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        @Size(min = 64, max = 64, message = "Current password format invalid")
        @Pattern(regexp = "^[a-f0-9]{64}$", message = "Current password format invalid")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 64, max = 64, message = "New password format invalid")
        @Pattern(regexp = "^[a-f0-9]{64}$", message = "New password format invalid")
        String newPassword
) {
}

