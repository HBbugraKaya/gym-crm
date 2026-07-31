package com.example.gymcrm.web.dto;

import java.time.LocalDate;

public record TraineeUpdateRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active) {
}
