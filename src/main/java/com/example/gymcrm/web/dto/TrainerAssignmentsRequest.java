package com.example.gymcrm.web.dto;

import java.util.List;

public record TrainerAssignmentsRequest(List<String> trainerUsernames) {
}