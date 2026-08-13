package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddTrainingRequest(@NotBlank String traineeUsername,
                                 @NotBlank String trainerUsername,
                                 @NotBlank String trainingName,
                                 @NotNull TrainingTypeName trainingType,
                                 @NotNull LocalDate trainingDate,
                                 @NotNull int duration) {
}
