package com.veltro.inventory.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating a worker's role.
 */
public record UpdateRoleRequest(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(CASHIER|WAREHOUSE)$", message = "Role must be CASHIER or WAREHOUSE")
        String role
) {
}