package com.veltro.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/v1/auth/change-password}.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 20, message = "New password must be between 8 and 20 characters")
        @Pattern(regexp = "^\\S+$", message = "Password must not contain whitespace")
        String newPassword
) {
}
