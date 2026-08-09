package com.example.gymcrm.report.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gymcrm.security.jwt")
public record ReportSecurityProperties(@NotBlank String secret, @NotBlank String issuer) {
}
