package com.example.gymcrm.workload.web.dto;

public record MonthlyWorkloadResponse(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        boolean active,
        int year,
        int month,
        int trainingDurationMinutes) {
}
