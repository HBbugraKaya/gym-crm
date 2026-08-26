package com.example.gymcrm.workload.service;

import com.example.gymcrm.workload.domain.TrainerWorkload;
import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.repository.TrainerWorkloadRepository;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {
    @Mock
    private TrainerWorkloadRepository trainerWorkloadRepository;

    private TrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new TrainerWorkloadService(trainerWorkloadRepository);
    }

    @Test
    void applyAddAndDeleteUpdatesMonthlySummary() {
        TrainerWorkload workload = new TrainerWorkload("Coach.One", "Coach", "One", true);
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase(anyString()))
                .thenReturn(Optional.of(workload));

        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));
        service.apply(request("coach.one", LocalDate.of(2026, 8, 20), 30, false, TrainerWorkloadRequest.WorkloadAction.ADD));

        MonthlyWorkloadResponse added = service.findMonthly("COACH.ONE", 2026, 8);

        assertThat(added.trainerUsername()).isEqualTo("coach.one");
        assertThat(added.active()).isFalse();
        assertThat(added.trainingDurationMinutes()).isEqualTo(75);

        service.apply(request("Coach.One", LocalDate.of(2026, 8, 20), 30, false, TrainerWorkloadRequest.WorkloadAction.DELETE));

        assertThat(service.findMonthly("Coach.One", 2026, 8).trainingDurationMinutes()).isEqualTo(45);
        verify(trainerWorkloadRepository, times(3)).save(workload);
    }

    @Test
    void applyAddCreatesAndSavesNewTrainerWorkload() {
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase("coach.one"))
                .thenReturn(Optional.empty());

        service.apply(request(
                "coach.one",
                LocalDate.of(2026, 8, 5),
                45,
                true,
                TrainerWorkloadRequest.WorkloadAction.ADD));

        ArgumentCaptor<TrainerWorkload> savedWorkload = ArgumentCaptor.forClass(TrainerWorkload.class);
        verify(trainerWorkloadRepository).save(savedWorkload.capture());
        assertThat(savedWorkload.getValue().getTrainerUsername()).isEqualTo("coach.one");
        assertThat(savedWorkload.getValue().durationFor(2026, 8)).isEqualTo(45);
    }

    @Test
    void summaryContainsYearsAndMonths() {
        givenExistingWorkload();

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
        givenExistingWorkload();

        service.apply(request("Coach.One", LocalDate.of(2026, 8, 5), 45, true, TrainerWorkloadRequest.WorkloadAction.ADD));

        assertThat(service.findMonthly("Coach.One", 2026, 9).trainingDurationMinutes()).isZero();
    }

    @Test
    void rejectsDeletingMoreMinutesThanRecorded() {
        givenExistingWorkload();

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
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase("Unknown.Coach"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findSummary("Unknown.Coach"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsDeletingUnknownTrainer() {
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase("Unknown.Coach"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(request(
                "Unknown.Coach",
                LocalDate.of(2026, 8, 5),
                45,
                true,
                TrainerWorkloadRequest.WorkloadAction.DELETE)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void propagatesDatabaseFailure() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase("Coach.One"))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.apply(request(
                "Coach.One",
                LocalDate.of(2026, 8, 5),
                45,
                true,
                TrainerWorkloadRequest.WorkloadAction.ADD)))
                .isSameAs(failure);
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

    private TrainerWorkload givenExistingWorkload() {
        TrainerWorkload workload = new TrainerWorkload("Coach.One", "Coach", "One", true);
        when(trainerWorkloadRepository.findByTrainerUsernameIgnoreCase(anyString()))
                .thenReturn(Optional.of(workload));
        return workload;
    }
}
