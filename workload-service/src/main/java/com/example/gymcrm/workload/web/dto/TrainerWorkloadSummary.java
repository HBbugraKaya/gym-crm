package com.example.gymcrm.workload.web.dto;

import java.util.List;

public record TrainerWorkloadSummary(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        boolean active,
        List<YearWorkloadSummary> years) {

    public record YearWorkloadSummary(int year, List<MonthWorkloadSummary> months) {
    }

    public record MonthWorkloadSummary(int month, int trainingDurationMinutes) {
    }
}
