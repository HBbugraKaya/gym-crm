package com.example.gymcrm.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Test
    void jwtDecoderUsesTheConfiguredIssuerContract() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        RevokedTokenValidator revokedTokenValidator = mock(RevokedTokenValidator.class);
        when(revokedTokenValidator.validate(any())).thenReturn(OAuth2TokenValidatorResult.success());
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt(SECRET, "configured-issuer", Duration.ofMinutes(15)),
                new SecurityProperties.Cors(List.of("http://localhost:3000")));
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(key, revokedTokenValidator, properties);
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));

        assertThat(decoder.decode(encode(encoder, "configured-issuer"))).isNotNull();
        assertThatThrownBy(() -> decoder.decode(encode(encoder, "other-issuer")))
                .isInstanceOf(JwtValidationException.class);
    }

    private String encode(JwtEncoder encoder, String issuer) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("john.smith")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .id("token-id")
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();
    }
}
