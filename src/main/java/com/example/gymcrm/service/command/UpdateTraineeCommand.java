package com.example.gymcrm.service.command;

import java.time.LocalDate;

public record UpdateTraineeCommand(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active
) {
}
