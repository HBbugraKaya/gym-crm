package com.example.gymcrm.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record TraineeRegistrationRequest(@NotBlank String firstName, @NotBlank String lastName, LocalDate dateOfBirth, String address) {

}
