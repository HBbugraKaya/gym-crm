package com.example.gymcrm.web.dto;

import java.time.LocalDate;

import com.example.gymcrm.entity.TrainingTypeName;

public record TrainerTrainingResponse(
        String trainingName,
        TrainingTypeName trainingType,
        LocalDate trainingDate,
        int durationMinute,
        String traineeName) {
}
