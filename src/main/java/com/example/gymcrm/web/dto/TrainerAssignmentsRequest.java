package com.example.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TrainerAssignmentsRequest(
        @NotNull List<@NotBlank String> trainerUsernames
) {
    public TrainerAssignmentsRequest {
        trainerUsernames = trainerUsernames == null ? null : List.copyOf(trainerUsernames);
    }
}
