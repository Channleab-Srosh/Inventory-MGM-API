package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    @Schema(description = "Full name of the customer", example = "Sophea Chan")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone number format is invalid")
    @Schema(description = "Customer primary contact phone number", example = "+85512345678")
    private String phone;

    @Email(message = "Email format is invalid")
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+\\-]+@gmail\\.com$", message = "Only Gmail addresses (@gmail.com) are allowed")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Customer email address (Gmail only)", example = "sophea.chan@gmail.com")
    private String email;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Physical address or delivery location", example = "No. 128, Street 271, Phnom Penh")
    private String address;
}
