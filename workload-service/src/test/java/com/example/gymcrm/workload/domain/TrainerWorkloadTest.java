package com.example.gymcrm.workload.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerWorkloadTest {
    @Test
    void addDurationCreatesYearAndMonthSummary() {
        TrainerWorkload workload = new TrainerWorkload("Coach.One", "Coach", "One", true);

        workload.addDuration(LocalDate.of(2026, 8, 9), 60);

        assertThat(workload.getId()).isEqualTo("coach.one");
        assertThat(workload.getYears()).hasSize(1);
        assertThat(workload.getYears().getFirst().getYear()).isEqualTo(2026);
        assertThat(workload.getYears().getFirst().getMonths()).hasSize(1);
        assertThat(workload.getYears().getFirst().getMonths().getFirst().getMonth()).isEqualTo(8);
        assertThat(workload.durationFor(2026, 8)).isEqualTo(60);
    }

    @Test
    void addDurationMergesDurationsForTheSameMonth() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);

        workload.addDuration(LocalDate.of(2026, 8, 9), 60);
        workload.addDuration(LocalDate.of(2026, 8, 20), 30);

        assertThat(workload.durationFor(2026, 8)).isEqualTo(90);
    }

    @Test
    void subtractDurationRemovesEmptyMonthAndYear() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);
        workload.addDuration(LocalDate.of(2026, 8, 9), 60);

        workload.subtractDuration(LocalDate.of(2026, 8, 9), 60);

        assertThat(workload.getYears()).isEmpty();
        assertThat(workload.durationFor(2026, 8)).isZero();
    }

    @Test
    void subtractDurationKeepsRemainingDuration() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);
        workload.addDuration(LocalDate.of(2026, 8, 9), 60);

        workload.subtractDuration(LocalDate.of(2026, 8, 9), 20);

        assertThat(workload.durationFor(2026, 8)).isEqualTo(40);
    }

    @Test
    void durationForUnknownMonthReturnsZero() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);

        assertThat(workload.durationFor(2026, 8)).isZero();
    }

    @Test
    void subtractDurationRejectsUnknownMonth() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);

        assertThatThrownBy(() -> workload.subtractDuration(LocalDate.of(2026, 8, 9), 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateProfileChangesTrainerIdentityAndStatus() {
        TrainerWorkload workload = new TrainerWorkload("coach.one", "Coach", "One", true);

        workload.updateProfile("coach.two", "New", "Coach", false);

        assertThat(workload.getTrainerUsername()).isEqualTo("coach.two");
        assertThat(workload.getTrainerFirstName()).isEqualTo("New");
        assertThat(workload.getTrainerLastName()).isEqualTo("Coach");
        assertThat(workload.isActive()).isFalse();
    }
}
