package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.security.CurrentUser;
import com.example.gymcrm.service.command.AddTrainingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingRepository trainingRepository;
    private final TraineeRepository traineeRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final CurrentUser currentUser;
    private final GymCrmMetrics metrics;

    public TrainingService(TrainingRepository trainingRepository,
                           TraineeRepository traineeRepository,
                           TrainingTypeRepository trainingTypeRepository,
                           CurrentUser currentUser,
                           GymCrmMetrics metrics) {
        this.trainingRepository = trainingRepository;
        this.traineeRepository = traineeRepository;
        this.trainingTypeRepository = trainingTypeRepository;
        this.currentUser = currentUser;
        this.metrics = metrics;
    }

    @Transactional
    public Training addTraining(AddTrainingCommand command) {
        Trainer trainer = currentUser.requireTrainer();
        requireSameTrainer(trainer.getUsername(), command.trainerUsername());

        Trainee trainee = traineeRepository.findByUsername(command.traineeUsername())
                .orElseThrow(() -> new EntityNotFoundException("Trainee", command.traineeUsername()));
        TrainingType trainingType = resolveTrainingType(command.trainingType(), trainer);

        trainee.assignTrainer(trainer);
        Training training = new Training(
                trainee,
                trainer,
                command.trainingName(),
                trainingType,
                command.trainingDate(),
                command.durationMinutes());

        Training saved = trainingRepository.save(training);
        metrics.recordTrainingCreated();
        LOGGER.info("Added training id={} traineeUsername={} trainerUsername={} type={}",
                saved.getId(), trainee.getUsername(), trainer.getUsername(), trainingType.getName());
        return saved;
    }

    private void requireSameTrainer(String authenticatedUsername, String trainerUsername) {
        if (!authenticatedUsername.equalsIgnoreCase(trainerUsername)) {
            throw new ValidationException("Authenticated trainer must match training trainer username");
        }
    }

    private TrainingType resolveTrainingType(TrainingTypeName requestedType, Trainer trainer) {
        if (requestedType == null) {
            return trainer.getSpecialization();
        }
        return trainingTypeRepository.findByName(requestedType)
                .orElseThrow(() -> new EntityNotFoundException("TrainingType", requestedType.name()));
    }
}
