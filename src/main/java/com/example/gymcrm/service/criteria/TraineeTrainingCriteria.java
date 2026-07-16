package com.example.gymcrm.service.criteria;

import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.exception.ValidationException;

import java.time.LocalDate;

public record TraineeTrainingCriteria(
        LocalDate fromDate,
        LocalDate toDate,
        String trainerName,
        TrainingTypeName trainingType
) {
    public TraineeTrainingCriteria {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("periodFrom must be on or before periodTo");
        }
    }

    public static TraineeTrainingCriteria empty() {
        return new TraineeTrainingCriteria(null, null, null, null);
    }
}
