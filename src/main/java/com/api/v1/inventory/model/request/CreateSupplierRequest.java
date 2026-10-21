package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    @Schema(description = "Supplier company or vendor name", example = "Tech Supply Co., Ltd")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone number format is invalid")
    @Schema(description = "Supplier contact phone number", example = "+85523999888")
    private String phone;

    @Email(message = "Email format is invalid")
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+\\-]+@gmail\\.com$", message = "Only Gmail addresses (@gmail.com) are allowed")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Supplier contact email (Gmail only)", example = "techsupply.kh@gmail.com")
    private String email;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Supplier office or warehouse address", example = "Building 12, Russian Federation Blvd, Phnom Penh")
    private String address;
}
