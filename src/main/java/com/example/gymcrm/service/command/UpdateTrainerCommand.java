package com.example.gymcrm.service.command;

public record UpdateTrainerCommand(
        String firstName,
        String lastName,
        boolean active
) {
}
