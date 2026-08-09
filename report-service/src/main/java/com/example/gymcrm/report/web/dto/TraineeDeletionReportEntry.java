package com.example.gymcrm.report.web.dto;

import java.time.Instant;

public record TraineeDeletionReportEntry(
        String traineeUsername,
        String traineeFirstName,
        String traineeLastName,
        boolean active,
        Instant deletedAt) {
}
