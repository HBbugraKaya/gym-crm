package com.example.gymcrm.service.criteria;

import com.example.gymcrm.exception.ValidationException;

import java.time.LocalDate;

public record TrainerTrainingCriteria(
        LocalDate fromDate,
        LocalDate toDate,
        String traineeName
) {
    public TrainerTrainingCriteria {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("periodFrom must be on or before periodTo");
        }
    }

    public static TrainerTrainingCriteria empty() {
        return new TrainerTrainingCriteria(null, null, null);
    }
}
