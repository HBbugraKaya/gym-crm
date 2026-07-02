package com.example.gymcrm.service.criteria;

import java.time.LocalDate;

public record TrainerTrainingCriteria(
        LocalDate fromDate,
        LocalDate toDate,
        String traineeName
) {
    public static TrainerTrainingCriteria empty() {
        return new TrainerTrainingCriteria(null, null, null);
    }
}
