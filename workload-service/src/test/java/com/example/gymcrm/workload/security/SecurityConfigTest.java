package com.example.gymcrm.workload.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Test
    void jwtDecoderAcceptsOnlyTheConfiguredIssuer() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(
                new WorkloadSecurityProperties(SECRET, "configured-issuer"));
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
