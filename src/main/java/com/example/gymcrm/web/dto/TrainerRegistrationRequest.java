package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

public record TrainerRegistrationRequest(String firstName, String lastName, TrainingTypeName specialization) {

}
