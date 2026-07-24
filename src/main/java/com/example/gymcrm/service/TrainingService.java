package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return saved;
    }
}
