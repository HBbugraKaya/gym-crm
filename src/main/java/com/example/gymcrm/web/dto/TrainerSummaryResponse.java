package com.example.gymcrm.web.dto;

import com.example.gymcrm.entity.TrainingTypeName;

public record TrainerSummaryResponse(String username, String firstName, String lastName, TrainingTypeName specialization) {
}
