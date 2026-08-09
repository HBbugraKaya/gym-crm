package com.example.gymcrm.workload.service;

import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerWorkloadServiceTest {
    private final TrainerWorkloadService service = new TrainerWorkloadService();

    @Test
    void applyAddAndDeleteUpdatesMonthlySummary() {
        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));
        service.apply(request("coach.one", LocalDate.of(2026, 8, 20), 30, false, TrainerWorkloadRequest.WorkloadAction.ADD));

        MonthlyWorkloadResponse added = service.findMonthly("COACH.ONE", 2026, 8);

        assertThat(added.trainerUsername()).isEqualTo("coach.one");
        assertThat(added.active()).isFalse();
        assertThat(added.trainingDurationMinutes()).isEqualTo(75);

        service.apply(request("Coach.One", LocalDate.of(2026, 8, 20), 30, false, TrainerWorkloadRequest.WorkloadAction.DELETE));

        assertThat(service.findMonthly("Coach.One", 2026, 8).trainingDurationMinutes()).isEqualTo(45);
    }

    @Test
    void summaryContainsYearsAndMonths() {
        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));
        service.apply(request("Coach.One", LocalDate.of(2027, 1, 5), 60, true, TrainerWorkloadRequest.WorkloadAction.ADD));

        TrainerWorkloadSummary summary = service.findSummary("Coach.One");

        assertThat(summary.years())
                .extracting(TrainerWorkloadSummary.YearWorkloadSummary::year)
                .containsExactly(2026, 2027);
        assertThat(summary.years().getFirst().months().getFirst().trainingDurationMinutes()).isEqualTo(45);
    }

    @Test
    void findMonthlyReturnsZeroForKnownTrainerWithoutThatMonth() {
        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));

        assertThat(service.findMonthly("Coach.One", 2026, 9).trainingDurationMinutes()).isZero();
    }

    @Test
    void rejectsDeletingMoreMinutesThanRecorded() {
        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));

        assertThatThrownBy(() -> service.apply(
                request("Coach.One", LocalDate.of(2026, 8, 5), 60, true, TrainerWorkloadRequest.WorkloadAction.DELETE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsInvalidMonth() {
        assertThatThrownBy(() -> service.findMonthly("Coach.One", 2026, 13))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsUnknownTrainer() {
        assertThatThrownBy(() -> service.findSummary("Unknown.Coach"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private TrainerWorkloadRequest request(
            String username,
            LocalDate date,
            int duration,
            boolean active,
            TrainerWorkloadRequest.WorkloadAction action) {
        return new TrainerWorkloadRequest(
                username,
                "Coach",
                "One",
                active,
                date,
                duration,
                action);
    }
}
