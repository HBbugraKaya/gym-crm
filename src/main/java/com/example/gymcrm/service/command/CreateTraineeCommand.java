package com.example.gymcrm.service.command;

import java.time.LocalDate;

public record CreateTraineeCommand(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active
) {
}
