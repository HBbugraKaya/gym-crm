package com.example.gymcrm.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record TraineeUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active) {
}
