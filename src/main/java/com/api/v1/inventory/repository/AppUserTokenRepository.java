package com.api.v1.inventory.repository;

import com.api.v1.inventory.model.entity.AppUserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserTokenRepository extends JpaRepository<AppUserToken, Long> {

    Optional<AppUserToken> findByTokenAndRevokedFalse(String token);

    @org.springframework.transaction.annotation.Transactional
    void deleteByUserId(Long userId);
}
