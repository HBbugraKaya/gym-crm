package com.example.gymcrm.integration.jms;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gymcrm.jms.queues")
public record JmsQueueProperties(
        @NotBlank String trainerWorkload,
        @NotBlank String traineeDeletionReport) {
}
