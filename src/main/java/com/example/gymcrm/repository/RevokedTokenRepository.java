package com.example.gymcrm.repository;

import com.example.gymcrm.domain.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    long deleteByExpiresAtLessThanEqual(Instant expiresAt);
}
