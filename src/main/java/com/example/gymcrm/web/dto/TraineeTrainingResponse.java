package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

import java.time.LocalDate;

public record TraineeTrainingResponse(String trainingName,
                                      TrainingTypeName trainingType,
                                      LocalDate trainingDate,
                                      int durationMinutes,
                                      String trainerName) {
}
