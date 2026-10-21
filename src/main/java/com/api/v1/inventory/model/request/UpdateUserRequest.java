package com.api.v1.inventory.model.request;

import com.api.v1.inventory.model.enumerate.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateUserRequest {

    @Email(message = "Email must be a valid email format")
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+\\-]+@gmail\\.com$", message = "Only Gmail addresses (@gmail.com) are allowed")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Schema(description = "Updated user email address (Gmail only)", example = "sokha.mean@gmail.com")
    private String email;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @Schema(description = "Updated password (leave empty to keep unchanged)", example = "NewSecurePass@123")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Schema(description = "Updated full name of the user", example = "Sokha Mean")
    private String fullName;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Updated contact phone number", example = "+85512987654")
    private String phone;

    @NotNull(message = "Role is required")
    @Schema(description = "Updated access role assigned to the user", example = "MANAGER")
    private Role role;
}
