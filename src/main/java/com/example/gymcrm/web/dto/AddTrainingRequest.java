package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

import java.time.LocalDate;

public record AddTrainingRequest(String traineeUsername,
                                 String trainerUsername,
                                 String trainingName,
                                 TrainingTypeName trainingType,
                                 LocalDate trainingDate,
                                 int duration) {
}
