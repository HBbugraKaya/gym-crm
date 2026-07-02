package com.example.gymcrm.service.criteria;

import com.example.gymcrm.domain.TrainingTypeName;

import java.time.LocalDate;

public record TraineeTrainingCriteria(
        LocalDate fromDate,
        LocalDate toDate,
        String trainerName,
        TrainingTypeName trainingType
) {
    public static TraineeTrainingCriteria empty() {
        return new TraineeTrainingCriteria(null, null, null, null);
    }
}
