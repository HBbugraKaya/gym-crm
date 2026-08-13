package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainerRegistrationRequest(@NotBlank String firstName, @NotBlank String lastName, @NotNull TrainingTypeName specialization) {

}
