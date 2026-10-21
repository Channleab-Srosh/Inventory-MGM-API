package com.api.v1.inventory.model.response;

import com.api.v1.inventory.model.entity.Customer;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
