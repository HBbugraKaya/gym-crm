package com.example.gymcrm.service.command;

import com.example.gymcrm.domain.TrainingTypeName;

public record UpdateTrainerCommand(
        String firstName,
        String lastName,
        TrainingTypeName specialization,
        boolean active
) {
}
