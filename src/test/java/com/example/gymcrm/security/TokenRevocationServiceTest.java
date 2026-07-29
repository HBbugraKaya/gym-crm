package com.example.gymcrm.security;

import com.example.gymcrm.domain.RevokedToken;
import com.example.gymcrm.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenRevocationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Test
    void revokeStoresOnlyUnexpiredTokensWithRequiredClaims() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        var service = new TokenRevocationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        Jwt token = jwt("token-id", NOW.plusSeconds(60));

        service.revoke(token);

        org.mockito.ArgumentCaptor<RevokedToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(RevokedToken.class);
        verify(repository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenId()).isEqualTo("token-id");
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void revokeIgnoresExpiredTokensAndRejectsMissingRequiredClaims() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        var service = new TokenRevocationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        service.revoke(jwt("expired-id", NOW));

        assertThatIllegalArgumentException().isThrownBy(() -> service.revoke(
                Jwt.withTokenValue("missing-id").header("alg", "HS256").expiresAt(NOW.plusSeconds(1)).build()))
                .withMessage("JWT must contain jti and exp claims");
        verify(repository, never()).save(any());
    }

    @Test
    void isRevokedReturnsFalseForMissingIdAndDelegatesKnownIds() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        var service = new TokenRevocationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.existsById("token-id")).thenReturn(true);

        assertThat(service.isRevoked(null)).isFalse();
        assertThat(service.isRevoked("token-id")).isTrue();
        verify(repository).existsById("token-id");
    }

    private Jwt jwt(String tokenId, Instant expiresAt) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("jti", tokenId)
                .expiresAt(expiresAt)
                .build();
    }
}
