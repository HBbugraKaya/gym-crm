package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.integration.TrainerWorkloadClient;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final GymCrmMetrics metrics;
    private final TrainerWorkloadClient trainerWorkloadClient;

    @Transactional
    @PreAuthorize("hasRole('TRAINER') and #trainerUsername.equalsIgnoreCase(authentication.name)")
    public Training addTraining(
            String traineeUsername,
            String trainerUsername,
            String trainingName,
            LocalDate trainingDate,
            int durationMinutes) {
        Trainer trainer = trainerRepository.findByUserUsernameIgnoreCase(trainerUsername)
                .orElseThrow(() -> new EntityNotFoundException("Trainer", trainerUsername));
        Trainee trainee = traineeRepository.findByUserUsernameIgnoreCase(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException("Trainee", traineeUsername));

        trainee.assignTrainer(trainer);
        Training training = new Training(
                trainee,
                trainer,
                trainingName,
                trainer.getSpecialization(),
                trainingDate,
                durationMinutes);

        Training saved = trainingRepository.save(training);
        metrics.recordTrainingCreated();
        trainerWorkloadClient.synchronize(workloadRequest(saved, TrainerWorkloadRequest.WorkloadAction.ADD));
        return saved;
    }

    @Transactional
    @PreAuthorize("hasRole('TRAINER')")
    public void deleteTraining(Long trainingId) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Training", trainingId.toString()));
        verifyTrainerOwnsTraining(training);

        trainingRepository.delete(training);
        trainerWorkloadClient.synchronize(workloadRequest(training, TrainerWorkloadRequest.WorkloadAction.DELETE));
    }

    private TrainerWorkloadRequest workloadRequest(
            Training training,
            TrainerWorkloadRequest.WorkloadAction action) {
        Trainer trainer = training.getTrainer();
        return new TrainerWorkloadRequest(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.isActive(),
                training.getDate(),
                training.getDurationMinutes(),
                action);
    }

    private void verifyTrainerOwnsTraining(Training training) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !training.getTrainer().getUsername().equalsIgnoreCase(authentication.getName())) {
            throw new AccessDeniedException("Training belongs to another trainer");
        }
    }
}
