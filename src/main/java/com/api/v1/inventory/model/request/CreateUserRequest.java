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
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique username for authentication")
    private String username;

    @Email(message = "Email must be a valid email format")
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+\\-]+@gmail\\.com$", message = "Only Gmail addresses (@gmail.com) are allowed")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Schema(description = "User email address (Gmail only)")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @Schema(description = "Account password")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Schema(description = "Full name of the user")
    private String fullName;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Contact phone number")
    private String phone;

    @NotNull(message = "Role is required")
    @Schema(description = "Access role assigned to the user")
    private Role role;
}
