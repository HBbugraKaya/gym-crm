package com.example.gymcrm.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TrainerWorkloadRequest(
                @NotBlank(message = "Trainer username is required") String trainerUsername,

                @NotBlank(message = "Trainer first name is required") String trainerFirstName,

                @NotBlank(message = "Trainer last name is required") String trainerLastName,

                @NotNull(message = "Is active status is required") Boolean isActive,

                @NotNull(message = "Training date is required") LocalDate trainingDate,

                @Positive(message = "Training duration must be positive") int trainingDuration,

                @NotNull(message = "Action type is required") ActionType actionType) {
}
