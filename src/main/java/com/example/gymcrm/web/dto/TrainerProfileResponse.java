package com.example.gymcrm.web.dto;

import com.example.gymcrm.domain.TrainingTypeName;

import java.util.List;

public record TrainerProfileResponse(
        String username,
        String firstName,
        String lastName,
        TrainingTypeName specialization,
        boolean active,
        List<TraineeSummaryResponse> trainees
) {
    public TrainerProfileResponse {
        trainees = List.copyOf(trainees);
    }
}
