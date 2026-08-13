package com.example.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TrainerUpdateRequest(@NotBlank String firstName, @NotBlank String lastName, boolean active) {

}
