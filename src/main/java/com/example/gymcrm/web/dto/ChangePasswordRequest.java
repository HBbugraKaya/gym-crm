package com.example.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) String oldPassword,
        @NotBlank @Size(max = 128) String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[newPassword=<redacted>]";
    }
}
