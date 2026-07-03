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

import java.time.LocalDate;
import java.util.List;

import static com.example.gymcrm.service.ValidationSupport.requireNonNull;
import static com.example.gymcrm.service.ValidationSupport.requirePositive;
import static com.example.gymcrm.service.ValidationSupport.requireText;

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
        requireNonNull(command, "command");
        String traineeUsername = requireText(command.traineeUsername(), "traineeUsername");
        String trainerUsername = requireText(command.trainerUsername(), "trainerUsername");
        String trainingName = requireText(command.trainingName(), "trainingName");
        TrainingTypeName trainingTypeName = requireNonNull(command.trainingType(), "trainingType");
        LocalDate trainingDate = requireNonNull(command.trainingDate(), "trainingDate");
        int durationMinutes = requirePositive(command.durationMinutes(), "durationMinutes");

        Trainer trainer = authenticationService.authenticateTrainer(trainerCredentials);
        requireSameTrainer(trainer.getUsername(), trainerUsername);

        Trainee trainee = traineeRepository.findByUsername(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException("Trainee", traineeUsername));
        TrainingType trainingType = trainingTypeRepository.findByName(trainingTypeName)
                .orElseThrow(() -> new EntityNotFoundException("TrainingType", trainingTypeName.name()));

        trainee.assignTrainer(trainer);
        Training training = new Training(
                trainee,
                trainer,
                trainingName,
                trainingType,
                trainingDate,
                durationMinutes);

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
        String requested = requireText(trainerUsername, "trainerUsername");
        if (!authenticatedUsername.equalsIgnoreCase(requested)) {
            throw new ValidationException("Authenticated trainer must match training trainer username");
        }
    }
}
