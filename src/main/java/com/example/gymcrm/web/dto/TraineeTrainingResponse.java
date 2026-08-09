package com.example.gymcrm.web.dto;

import com.example.gymcrm.domain.TrainingTypeName;

import java.time.LocalDate;

public record TraineeTrainingResponse(
        Long trainingId,
        String trainingName,
        LocalDate trainingDate,
        TrainingTypeName trainingType,
        int durationMinutes,
        String trainerName
) {
    public TraineeTrainingResponse(
            String trainingName,
            LocalDate trainingDate,
            TrainingTypeName trainingType,
            int durationMinutes,
            String trainerName) {
        this(null, trainingName, trainingDate, trainingType, durationMinutes, trainerName);
    }
}
