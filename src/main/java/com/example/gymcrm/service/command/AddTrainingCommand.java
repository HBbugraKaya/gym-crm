package com.example.gymcrm.service.command;

import com.example.gymcrm.domain.TrainingTypeName;

import java.time.LocalDate;

public record AddTrainingCommand(
        String traineeUsername,
        String trainerUsername,
        String trainingName,
        TrainingTypeName trainingType,
        LocalDate trainingDate,
        int durationMinutes
) {
    public AddTrainingCommand(String traineeUsername,
                              String trainerUsername,
                              String trainingName,
                              LocalDate trainingDate,
                              int durationMinutes) {
        this(traineeUsername, trainerUsername, trainingName, null, trainingDate, durationMinutes);
    }
}
