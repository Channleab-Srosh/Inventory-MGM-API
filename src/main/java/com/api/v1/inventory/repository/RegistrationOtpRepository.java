package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.RegistrationOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationOtp r WHERE LOWER(r.email) = LOWER(:email) AND r.used = false ORDER BY r.id DESC")
    List<RegistrationOtp> findActiveOtpsByEmailForUpdate(@Param("email") String email);

    List<RegistrationOtp> findByEmailIgnoreCaseAndUsedFalseOrderByIdDesc(String email);

    @Transactional
    @Modifying
    @Query("UPDATE RegistrationOtp r SET r.used = true WHERE LOWER(r.email) = LOWER(:email) AND r.used = false")
    void invalidateAllActiveOtpsByEmail(@Param("email") String email);

    /**
     * Returns true when the email already has an active (unused, non-expired) OTP
     * whose {@code lastRequestedAt} is after the given {@code after} timestamp.
     * Used to enforce the resend rate limit.
     */
    @Query("SELECT COUNT(r) > 0 FROM RegistrationOtp r " +
           "WHERE LOWER(r.email) = LOWER(:email) " +
           "AND r.used = false " +
           "AND r.expiresAt > :now " +
           "AND r.lastRequestedAt > :after")
    boolean existsRecentActiveOtp(@Param("email") String email,
                                  @Param("now") Instant now,
                                  @Param("after") Instant after);
}
