package com.example.gymcrm.web.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull Boolean active) {
}
