package com.example.gymcrm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.gymcrm.entity.TrainerWorkload;
import com.example.gymcrm.repository.TrainerWorkloadRepository;
import com.example.gymcrm.web.dto.ActionType;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;

    public void processWorkload(TrainerWorkloadRequest request) {
        log.info("Processing workload for trainer: {}, action: {}, duration: {} min",
                request.trainerUsername(), request.actionType(), request.trainingDuration());

        int delta = request.actionType() == ActionType.ADD
                ? request.trainingDuration()
                : -request.trainingDuration();

        Optional<TrainerWorkload> found = repository.findById(request.trainerUsername());
        if (found.isEmpty() && delta < 0) {
            log.warn("Cannot deduct duration. Trainer not found: {}", request.trainerUsername());
            return;
        }

        TrainerWorkload current = found.orElseGet(() -> new TrainerWorkload(
                request.trainerUsername(),
                request.trainerFirstName(),
                request.trainerLastName(),
                request.isActive(),
                List.of()));

        repository.save(current.adjust(
                request.trainerFirstName(),
                request.trainerLastName(),
                request.isActive(),
                request.trainingDate().getYear(),
                request.trainingDate().getMonthValue(),
                delta));

        log.info("Successfully updated workload in MongoDB for trainer: {}", request.trainerUsername());
    }

    public int getMonthlyDuration(String username, int year, int month) {
        return repository.findById(username)
                .map(workload -> workload.duration(year, month))
                .orElse(0);
    }
}
