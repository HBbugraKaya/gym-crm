package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingServiceImpl implements TrainingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingServiceImpl.class);

    private final TrainingRepository trainingRepository;
    private final TraineeRepository traineeRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final AuthenticationService authenticationService;

    public TrainingServiceImpl(TrainingRepository trainingRepository,
                               TraineeRepository traineeRepository,
                               TrainingTypeRepository trainingTypeRepository,
                               AuthenticationService authenticationService) {
        this.trainingRepository = trainingRepository;
        this.traineeRepository = traineeRepository;
        this.trainingTypeRepository = trainingTypeRepository;
        this.authenticationService = authenticationService;
    }

    @Override
    @Transactional
    public Training addTraining(Credentials trainerCredentials, AddTrainingCommand command) {
        Trainer trainer = authenticationService.authenticateTrainer(trainerCredentials);
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
        LOGGER.info("Added training id={} traineeUsername={} trainerUsername={} type={}",
                saved.getId(), trainee.getUsername(), trainer.getUsername(), trainingType.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> findAll() {
        return trainingRepository.findAll();
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
