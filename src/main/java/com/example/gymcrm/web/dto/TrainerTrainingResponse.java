package com.example.gymcrm.web.dto;

import com.example.gymcrm.domain.TrainingTypeName;

import java.time.LocalDate;

public record TrainerTrainingResponse(
        String trainingName,
        LocalDate trainingDate,
        TrainingTypeName trainingType,
        int durationMinutes,
        String traineeName
) {
}
