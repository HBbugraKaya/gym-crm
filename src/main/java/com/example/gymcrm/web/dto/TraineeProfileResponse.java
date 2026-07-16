package com.example.gymcrm.web.dto;

import java.time.LocalDate;
import java.util.List;

public record TraineeProfileResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active,
        List<TrainerSummaryResponse> trainers
) {
    public TraineeProfileResponse {
        trainers = List.copyOf(trainers);
    }
}
