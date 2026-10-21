package com.api.v1.inventory.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerStatusRequest {

    @NotNull(message = "Active status is required")
    @Schema(description = "Customer active status", example = "true")
    private Boolean active;
}
