package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.AppUserToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserTokenRepository extends JpaRepository<AppUserToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM AppUserToken t WHERE t.user.id = :userId AND t.revoked = false ORDER BY t.id DESC")
    List<AppUserToken> findActiveTokensByUserIdForUpdate(@Param("userId") Long userId);

    List<AppUserToken> findByUserIdAndRevokedFalse(Long userId);

    Optional<AppUserToken> findByTokenAndRevokedFalse(String token);

    @Transactional
    void deleteByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE AppUserToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllActiveTokensByUserId(@Param("userId") Long userId);
}
