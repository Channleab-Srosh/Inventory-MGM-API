package com.api.v1.inventory.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "registration_otps", indexes = {
        @Index(name = "idx_registration_otps_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    /** BCrypt hash of the 6-digit OTP – never store the raw code. */
    @Column(nullable = false, length = 60)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    /** Number of incorrect verification attempts. */
    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    /** Timestamp of the most-recent OTP send for this email – used to enforce
     *  the resend rate limit. */
    @Column(name = "last_requested_at", nullable = false)
    private Instant lastRequestedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
