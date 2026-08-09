package com.example.gymcrm.security;

import com.example.gymcrm.domain.RevokedToken;
import com.example.gymcrm.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {
    private final RevokedTokenRepository revokedTokenRepository;
    private final Clock clock;

    @Transactional
    public void revoke(Jwt token) {
        String tokenId = token.getId();
        Instant expiresAt = token.getExpiresAt();
        if (tokenId == null || expiresAt == null) {
            throw new IllegalArgumentException("JWT must contain jti and exp claims");
        }
        if (expiresAt.isAfter(clock.instant())) {
            revokedTokenRepository.save(new RevokedToken(tokenId, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return tokenId != null && revokedTokenRepository.existsById(tokenId);
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    @Transactional
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteByExpiresAtLessThanEqual(clock.instant());
    }
}
