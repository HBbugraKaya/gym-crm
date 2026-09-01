package com.example.gymcrm.workload.service;

import com.example.gymcrm.workload.domain.TrainerWorkload;
import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.repository.TrainerWorkloadRepository;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TrainerWorkloadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final TrainerWorkloadRepository trainerWorkloadRepository;

    public void apply(TrainerWorkloadRequest request) {
        LOGGER.debug(
                "Starting workload operation action={} trainerUsername={}",
                request.action(),
                request.trainerUsername());
        try {
            TrainerWorkload workload = trainerWorkloadRepository
                    .findByTrainerUsernameIgnoreCase(request.trainerUsername())
                    .orElseGet(() -> createWorkloadFor(request));

            updateDuration(workload, request.trainingDate(), request.trainingDurationMinutes(), request.action());
            workload.updateProfile(
                    request.trainerUsername(),
                    request.trainerFirstName(),
                    request.trainerLastName(),
                    request.active());
            trainerWorkloadRepository.save(workload);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "Workload operation failed action={} trainerUsername={} failureType={}",
                    request.action(),
                    request.trainerUsername(),
                    exception.getClass().getSimpleName());
            throw exception;
        }

        LOGGER.info(
                "Completed workload operation action={} trainerUsername={}",
                request.action(),
                request.trainerUsername());
        LOGGER.debug(
                "Workload month updated trainerUsername={} month={} durationMinutes={}",
                request.trainerUsername(),
                request.trainingDate().withDayOfMonth(1),
                request.trainingDurationMinutes());
    }

    public MonthlyWorkloadResponse findMonthly(String trainerUsername, int year, int month) {
        validateMonth(month);
        LOGGER.debug(
                "Reading monthly workload trainerUsername={} year={} month={}",
                trainerUsername,
                year,
                month);
        TrainerWorkload workload = findWorkload(trainerUsername);
        return new MonthlyWorkloadResponse(
                workload.getTrainerUsername(),
                workload.getTrainerFirstName(),
                workload.getTrainerLastName(),
                workload.isActive(),
                year,
                month,
                workload.durationFor(year, month));
    }

    public TrainerWorkloadSummary findSummary(String trainerUsername) {
        LOGGER.debug("Reading trainer workload summary trainerUsername={}", trainerUsername);
        TrainerWorkload workload = findWorkload(trainerUsername);
        var years = workload.getYears().stream()
                .map(year -> new TrainerWorkloadSummary.YearWorkloadSummary(
                        year.getYear(),
                        year.getMonths().stream()
                                .map(month -> new TrainerWorkloadSummary.MonthWorkloadSummary(
                                        month.getMonth(),
                                        month.getTrainingSummaryDuration()))
                                .toList()))
                .toList();
        return new TrainerWorkloadSummary(
                workload.getTrainerUsername(),
                workload.getTrainerFirstName(),
                workload.getTrainerLastName(),
                workload.isActive(),
                years);
    }

    private void updateDuration(
            TrainerWorkload workload,
            LocalDate trainingDate,
            int durationMinutes,
            TrainerWorkloadRequest.WorkloadAction action) {
        if (action == TrainerWorkloadRequest.WorkloadAction.ADD) {
            workload.addDuration(trainingDate, durationMinutes);
            return;
        }

        int currentDuration = workload.durationFor(trainingDate.getYear(), trainingDate.getMonthValue());
        if (currentDuration < durationMinutes) {
            throw new ValidationException("Cannot delete more workload than has been recorded");
        }
        workload.subtractDuration(trainingDate, durationMinutes);
    }

    private TrainerWorkload findWorkload(String trainerUsername) {
        return trainerWorkloadRepository.findByTrainerUsernameIgnoreCase(trainerUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No workload exists for trainer " + trainerUsername));
    }

    private TrainerWorkload createWorkloadFor(TrainerWorkloadRequest request) {
        if (request.action() == TrainerWorkloadRequest.WorkloadAction.DELETE) {
            throw new EntityNotFoundException(
                    "No workload exists for trainer " + request.trainerUsername());
        }
        return new TrainerWorkload(
                request.trainerUsername(),
                request.trainerFirstName(),
                request.trainerLastName(),
                request.active());
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("month must be between 1 and 12");
        }
    }
}
