package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

public record TrainerProfileResponse(String username, String firstName, String lastName, TrainingTypeName specialization) {

}
