package com.veltro.inventory.dto.purchasing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new supplier (B2-04).
 */
public record CreateSupplierRequest(
        @NotBlank(message = "{validation.supplier.name.notblank}")
        @Size(max = 200, message = "{validation.supplier.name.size}")
        String name,

        @NotBlank(message = "{validation.supplier.taxid.notblank}")
        @Size(max = 50, message = "{validation.supplier.taxid.size}")
        String taxId,

        @Email(message = "{validation.supplier.email.format}")
        @Size(max = 200, message = "{validation.supplier.email.size}")
        String email,

        @Size(max = 20, message = "{validation.supplier.phone.size}")
        String phone,

        @Size(max = 500, message = "{validation.supplier.address.size}")
        String address,

        @Size(max = 500, message = "{validation.supplier.notes.size}")
        String notes
) {
}
