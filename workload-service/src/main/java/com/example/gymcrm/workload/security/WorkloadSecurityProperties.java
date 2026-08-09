package com.example.gymcrm.workload.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gymcrm.security.jwt")
public record WorkloadSecurityProperties(@NotBlank String secret, @NotBlank String issuer) {
}
