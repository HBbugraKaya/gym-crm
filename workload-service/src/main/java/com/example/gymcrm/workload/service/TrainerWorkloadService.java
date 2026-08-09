package com.example.gymcrm.workload.service;

import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrainerWorkloadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final Map<String, MutableTrainerWorkload> workloads = new ConcurrentHashMap<>();

    public void apply(TrainerWorkloadRequest request) {
        String key = normalizeUsername(request.trainerUsername());
        MutableTrainerWorkload workload = request.action() == TrainerWorkloadRequest.WorkloadAction.ADD
                ? workloads.computeIfAbsent(
                        key,
                        ignored -> new MutableTrainerWorkload(
                                request.trainerUsername(),
                                request.trainerFirstName(),
                                request.trainerLastName(),
                                request.active()))
                : findWorkload(request.trainerUsername());

        synchronized (workload) {
            updateDuration(workload, request.trainingDate(), request.trainingDurationMinutes(), request.action());
            workload.updateIdentity(
                    request.trainerUsername(),
                    request.trainerFirstName(),
                    request.trainerLastName(),
                    request.active());
        }

        LOGGER.info(
                "Trainer workload operation completed action={} trainerUsername={} month={} durationMinutes={}",
                request.action(),
                request.trainerUsername(),
                request.trainingDate().withDayOfMonth(1),
                request.trainingDurationMinutes());
    }

    public MonthlyWorkloadResponse findMonthly(String trainerUsername, int year, int month) {
        validateMonth(month);
        MutableTrainerWorkload workload = findWorkload(trainerUsername);

        synchronized (workload) {
            int duration = workload.durationFor(year, month);
            return new MonthlyWorkloadResponse(
                    workload.trainerUsername,
                    workload.trainerFirstName,
                    workload.trainerLastName,
                    workload.active,
                    year,
                    month,
                    duration);
        }
    }

    public TrainerWorkloadSummary findSummary(String trainerUsername) {
        MutableTrainerWorkload workload = findWorkload(trainerUsername);
        synchronized (workload) {
            return workload.toSummary();
        }
    }

    private void updateDuration(
            MutableTrainerWorkload workload,
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

    private MutableTrainerWorkload findWorkload(String trainerUsername) {
        MutableTrainerWorkload workload = workloads.get(normalizeUsername(trainerUsername));
        if (workload == null) {
            throw new EntityNotFoundException("No workload exists for trainer " + trainerUsername);
        }
        return workload;
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("month must be between 1 and 12");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private static final class MutableTrainerWorkload {
        private String trainerUsername;
        private String trainerFirstName;
        private String trainerLastName;
        private boolean active;
        private final Map<Integer, Map<Integer, Integer>> durationsByYear = new TreeMap<>();

        private MutableTrainerWorkload(
                String trainerUsername,
                String trainerFirstName,
                String trainerLastName,
                boolean active) {
            updateIdentity(trainerUsername, trainerFirstName, trainerLastName, active);
        }

        private void updateIdentity(
                String trainerUsername,
                String trainerFirstName,
                String trainerLastName,
                boolean active) {
            this.trainerUsername = trainerUsername;
            this.trainerFirstName = trainerFirstName;
            this.trainerLastName = trainerLastName;
            this.active = active;
        }

        private void addDuration(LocalDate date, int durationMinutes) {
            durationsByYear
                    .computeIfAbsent(date.getYear(), ignored -> new TreeMap<>())
                    .merge(date.getMonthValue(), durationMinutes, Integer::sum);
        }

        private void subtractDuration(LocalDate date, int durationMinutes) {
            Map<Integer, Integer> durationsByMonth = durationsByYear.get(date.getYear());
            int remaining = durationsByMonth.get(date.getMonthValue()) - durationMinutes;
            if (remaining == 0) {
                durationsByMonth.remove(date.getMonthValue());
            } else {
                durationsByMonth.put(date.getMonthValue(), remaining);
            }
            if (durationsByMonth.isEmpty()) {
                durationsByYear.remove(date.getYear());
            }
        }

        private int durationFor(int year, int month) {
            return durationsByYear.getOrDefault(year, Map.of()).getOrDefault(month, 0);
        }

        private TrainerWorkloadSummary toSummary() {
            var years = durationsByYear.entrySet().stream()
                    .map(yearEntry -> new TrainerWorkloadSummary.YearWorkloadSummary(
                            yearEntry.getKey(),
                            yearEntry.getValue().entrySet().stream()
                                    .map(monthEntry -> new TrainerWorkloadSummary.MonthWorkloadSummary(
                                            monthEntry.getKey(),
                                            monthEntry.getValue()))
                                    .toList()))
                    .toList();
            return new TrainerWorkloadSummary(
                    trainerUsername,
                    trainerFirstName,
                    trainerLastName,
                    active,
                    years);
        }
    }
}
