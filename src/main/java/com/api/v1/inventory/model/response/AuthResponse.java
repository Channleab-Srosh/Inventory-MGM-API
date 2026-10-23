package com.api.v1.inventory.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    @Builder.Default
    private String unit = "seconds";
    private String username;
    private String role;
    @Schema(description = "Indicates whether the user must change their password on first login", example = "false")
    private boolean mustChangePassword;
}
