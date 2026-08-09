package com.example.gymcrm.report.service;

import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TraineeReportServiceTest {
    private final Instant now = Instant.parse("2026-08-09T10:00:00Z");
    private final TraineeReportService service = new TraineeReportService(
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void recordDeletionStoresReportWithoutCredentials() {
        service.recordDeletion(new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true));

        assertThat(service.findAll()).singleElement().satisfies(report -> {
            assertThat(report.traineeUsername()).isEqualTo("runner.one");
            assertThat(report.traineeFirstName()).isEqualTo("Runner");
            assertThat(report.traineeLastName()).isEqualTo("One");
            assertThat(report.active()).isTrue();
            assertThat(report.deletedAt()).isEqualTo(now);
        });
    }

    @Test
    void findAllReturnsIndependentSnapshot() {
        assertThat(service.findAll()).isEmpty();
    }
}
