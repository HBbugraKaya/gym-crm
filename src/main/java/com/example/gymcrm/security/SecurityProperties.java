package com.example.gymcrm.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "gymcrm.security")
public record SecurityProperties(@NotNull Jwt jwt, @NotNull Cors cors) {

    public record Jwt(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration accessTokenTtl) {
    }

    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }
}
