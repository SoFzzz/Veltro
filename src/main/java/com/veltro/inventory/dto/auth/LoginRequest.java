package com.veltro.inventory.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/auth/login}.
 */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 64, max = 64, message = "Password format invalid")
        @Pattern(regexp = "^[a-f0-9]{64}$", message = "Password format invalid")
        String password
) {
}

