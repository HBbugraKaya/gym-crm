package com.example.gymcrm.web.dto;

import com.example.gymcrm.domain.TrainingTypeName;

import java.time.LocalDate;

public record TrainerTrainingResponse(
        Long trainingId,
        String trainingName,
        LocalDate trainingDate,
        TrainingTypeName trainingType,
        int durationMinutes,
        String traineeName
) {
    public TrainerTrainingResponse(
            String trainingName,
            LocalDate trainingDate,
            TrainingTypeName trainingType,
            int durationMinutes,
            String traineeName) {
        this(null, trainingName, trainingDate, trainingType, durationMinutes, traineeName);
    }
}
