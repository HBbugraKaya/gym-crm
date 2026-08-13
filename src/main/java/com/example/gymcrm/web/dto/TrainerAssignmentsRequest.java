package com.example.gymcrm.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record TrainerAssignmentsRequest(@NotNull List<String> trainerUsernames) {
}
