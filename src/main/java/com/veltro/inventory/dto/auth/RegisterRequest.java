package com.veltro.inventory.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 64, max = 64, message = "Password format invalid")
        @Pattern(regexp = "^[a-f0-9]{64}$", message = "Password format invalid")
        String password,

        String role,

        /** Business name — required for ADMIN registration, ignored for worker creation. */
        @Size(max = 50, message = "Business name must be at most 50 characters")
        String businessName
) {}

