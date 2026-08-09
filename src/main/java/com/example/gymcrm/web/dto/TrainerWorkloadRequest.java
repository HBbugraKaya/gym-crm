package com.example.gymcrm.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record TrainerWorkloadRequest(
        @NotBlank String trainerUsername,
        @NotBlank String trainerFirstName,
        @NotBlank String trainerLastName,
        @JsonProperty("isActive")
        boolean active,
        @NotNull LocalDate trainingDate,
        @Positive int trainingDurationMinutes,
        @JsonProperty("actionType")
        @NotNull WorkloadAction action) {

    public enum WorkloadAction {
        ADD,
        DELETE
    }
}
