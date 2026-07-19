package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingRepository trainingRepository;
    private final TraineeRepository traineeRepository;
    private final CurrentUser currentUser;
    private final GymCrmMetrics metrics;

    public TrainingService(TrainingRepository trainingRepository,
                           TraineeRepository traineeRepository,
                           CurrentUser currentUser,
                           GymCrmMetrics metrics) {
        this.trainingRepository = trainingRepository;
        this.traineeRepository = traineeRepository;
        this.currentUser = currentUser;
        this.metrics = metrics;
    }

    @Transactional
    public Training addTraining(String traineeUsername, String trainerUsername, String trainingName,
                                LocalDate trainingDate, int durationMinutes) {
        Trainer trainer = currentUser.requireTrainer();
        SelfAccess.require(trainer.getUsername(), trainerUsername,
                "Authenticated trainer must match training trainer username");

        Trainee trainee = traineeRepository.findByUsername(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException("Trainee", traineeUsername));
        TrainingType trainingType = trainer.getSpecialization();

        trainee.assignTrainer(trainer);
        Training training = new Training(
                trainee,
                trainer,
                trainingName,
                trainingType,
                trainingDate,
                durationMinutes);

        Training saved = trainingRepository.save(training);
        metrics.recordTrainingCreated();
        LOGGER.info("Added training id={} traineeUsername={} trainerUsername={} type={}",
                saved.getId(), trainee.getUsername(), trainer.getUsername(), trainingType.getName());
        return saved;
    }
}
