package com.example.gymcrm.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {
    @Test
    void createAccessTokenEncodesExpectedSubjectRolesAndExpiry() {
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        Instant now = Instant.parse("2026-07-29T10:00:00Z");
        var properties = new SecurityProperties(
                new SecurityProperties.Jwt("test-secret", Duration.ofMinutes(15)),
                new SecurityProperties.Cors(List.of("http://localhost:3000")));
        var service = new JwtTokenService(jwtEncoder, properties, Clock.fixed(now, ZoneOffset.UTC));
        var authentication = UsernamePasswordAuthenticationToken.authenticated("john.smith", null,
                List.of(new SimpleGrantedAuthority("ROLE_TRAINEE"), new SimpleGrantedAuthority("ROLE_TRAINER")));
        Jwt encodedJwt = Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .subject("john.smith")
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);

        String token = service.createAccessToken(authentication);

        ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parameters.capture());
        assertThat(token).isEqualTo("access-token");
        assertThat(parameters.getValue().getJwsHeader().getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        assertThat(parameters.getValue().getClaims().getClaimAsString("iss")).isEqualTo("gym-crm");
        assertThat(parameters.getValue().getClaims().getSubject()).isEqualTo("john.smith");
        assertThat(parameters.getValue().getClaims().getIssuedAt()).isEqualTo(now);
        assertThat(parameters.getValue().getClaims().getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(parameters.getValue().getClaims().getClaimAsStringList("roles"))
                .containsExactly("ROLE_TRAINEE", "ROLE_TRAINER");
        assertThat(parameters.getValue().getClaims().getId()).isNotBlank();
    }
}
