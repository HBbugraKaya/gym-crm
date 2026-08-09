package com.example.gymcrm.integration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gymcrm.workload")
public record WorkloadServiceProperties(@NotBlank String serviceUrl) {
}
