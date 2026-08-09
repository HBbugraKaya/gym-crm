package com.example.gymcrm.integration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gymcrm.report")
public record ReportServiceProperties(@NotBlank String serviceUrl) {
}
