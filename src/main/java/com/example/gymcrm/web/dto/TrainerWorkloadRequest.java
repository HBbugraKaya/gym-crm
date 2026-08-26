package com.example.gymcrm.web.dto;

import java.time.LocalDate;


public record TrainerWorkloadRequest(
    String trainerUsername,
    String trainerFirstName,
    String trainerLastName,
    Boolean isActive,
    LocalDate trainingDate,
    int trainingDuration,
    ActionType actionType) {
}
