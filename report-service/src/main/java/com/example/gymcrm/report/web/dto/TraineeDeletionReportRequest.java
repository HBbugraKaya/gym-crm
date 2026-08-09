package com.example.gymcrm.report.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TraineeDeletionReportRequest(
        @NotBlank String traineeUsername,
        @NotBlank String traineeFirstName,
        @NotBlank String traineeLastName,
        @JsonProperty("isActive") boolean active) {
}
