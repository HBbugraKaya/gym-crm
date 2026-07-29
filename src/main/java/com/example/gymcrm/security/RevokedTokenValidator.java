package com.example.gymcrm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED_TOKEN = new OAuth2Error("invalid_token", "Token has been revoked", null);

    private final TokenRevocationService tokenRevocationService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return tokenRevocationService.isRevoked(token.getId())
                ? OAuth2TokenValidatorResult.failure(REVOKED_TOKEN)
                : OAuth2TokenValidatorResult.success();
    }
}
