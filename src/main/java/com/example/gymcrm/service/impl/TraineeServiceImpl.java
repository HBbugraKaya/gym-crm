package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.generator.PasswordGenerator;
import com.example.gymcrm.generator.UsernameGenerator;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.TraineeService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TraineeServiceImpl implements TraineeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeServiceImpl.class);

    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final TrainingRepository trainingRepository;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final AuthenticationService authenticationService;

    public TraineeServiceImpl(TraineeRepository traineeRepository,
                              TrainerRepository trainerRepository,
                              TrainingRepository trainingRepository,
                              UsernameGenerator usernameGenerator,
                              PasswordGenerator passwordGenerator,
                              AuthenticationService authenticationService) {
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
        this.trainingRepository = trainingRepository;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.authenticationService = authenticationService;
    }

    @Override
    @Transactional
    public Trainee create(CreateTraineeCommand command) {
        String username = usernameGenerator.generate(command.firstName(), command.lastName());
        String password = passwordGenerator.generate();

        Trainee trainee = new Trainee(
                new User(command.firstName(), command.lastName(), username, password, command.active()),
                command.dateOfBirth(),
                command.address());
        Trainee saved = traineeRepository.save(trainee);
        LOGGER.info("Created trainee id={} username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Trainee findByUsername(Credentials credentials, String username) {
        Trainee authenticated = authenticationService.authenticateTrainee(credentials);
        requireSameUser(authenticated.getUsername(), username, "trainee");
        authenticated.getTrainers().size();
        LOGGER.debug("Selected trainee profile username={}", authenticated.getUsername());
        return authenticated;
    }

    @Override
    @Transactional
    public Trainee update(Credentials credentials, String username, UpdateTraineeCommand command) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        requireSameUser(trainee.getUsername(), username, "trainee");
        trainee.updateProfile(
                command.firstName(), command.lastName(), command.dateOfBirth(), command.address(), command.active());
        trainee.getTrainers().size();
        LOGGER.info("Updated trainee id={} username={}", trainee.getId(), trainee.getUsername());
        return trainee;
    }

    @Override
    @Transactional
    public void changePassword(Credentials credentials, String newPassword) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        trainee.changePassword(newPassword);
        LOGGER.info("Changed trainee password id={} username={}", trainee.getId(), trainee.getUsername());
    }

    @Override
    @Transactional
    public Trainee activate(Credentials credentials) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        changeStatus(trainee, true);
        return trainee;
    }

    @Override
    @Transactional
    public Trainee deactivate(Credentials credentials) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        changeStatus(trainee, false);
        return trainee;
    }

    @Override
    @Transactional
    public void deleteByUsername(Credentials credentials, String username) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        requireSameUser(trainee.getUsername(), username, "trainee");
        trainee.clearTrainers();
        traineeRepository.delete(trainee);
        LOGGER.info("Deleted trainee id={} username={} with cascade trainings", trainee.getId(), trainee.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTrainings(Credentials credentials, String traineeUsername, TraineeTrainingCriteria criteria) {
        Trainee trainee = authenticationService.authenticateTrainee(credentials);
        requireSameUser(trainee.getUsername(), traineeUsername, "trainee");
        List<Training> trainings = trainingRepository.findByTraineeUsername(traineeUsername, criteria);
        LOGGER.debug("Loaded trainee trainings username={} count={}", traineeUsername, trainings.size());
        return trainings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(Credentials credentials, String traineeUsername) {
        Trainee trainee = traineeRepository.findByUsernameWithTrainers(
                        authenticationService.authenticateTrainee(credentials).getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Trainee", traineeUsername));
        requireSameUser(trainee.getUsername(), traineeUsername, "trainee");
        Set<Long> assignedTrainerIds = trainee.getTrainers().stream()
                .map(Trainer::getId)
                .collect(Collectors.toSet());
        List<Trainer> unassigned = trainerRepository.findAll().stream()
                .filter(Trainer::isActive)
                .filter(trainer -> !assignedTrainerIds.contains(trainer.getId()))
                .toList();
        LOGGER.debug("Loaded unassigned trainers for trainee username={} count={}", traineeUsername, unassigned.size());
        return unassigned;
    }

    @Override
    @Transactional
    public List<Trainer> updateTrainers(Credentials credentials, String traineeUsername, Collection<String> trainerUsernames) {
        Trainee trainee = traineeRepository.findByUsernameWithTrainers(
                        authenticationService.authenticateTrainee(credentials).getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Trainee", traineeUsername));
        requireSameUser(trainee.getUsername(), traineeUsername, "trainee");

        Set<String> requestedUsernames = trainerUsernames == null
                ? Set.of()
                : new LinkedHashSet<>(trainerUsernames);
        List<Trainer> trainers = trainerRepository.findAllByUsernames(requestedUsernames);
        validateAllTrainersFound(requestedUsernames, trainers);

        trainee.replaceTrainers(trainers);
        LOGGER.info("Updated trainee trainers traineeId={} username={} trainerCount={}",
                trainee.getId(), trainee.getUsername(), trainers.size());
        return List.copyOf(trainers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainee> findAll() {
        return traineeRepository.findAll();
    }

    private void changeStatus(Trainee trainee, boolean active) {
        if (trainee.isActive() == active) {
            LOGGER.warn("Trainee status change rejected id={} username={} active={}",
                    trainee.getId(), trainee.getUsername(), active);
            throw new ProfileStateException("Trainee is already " + (active ? "active" : "inactive"));
        }
        trainee.setActive(active);
        LOGGER.info("Changed trainee status id={} username={} active={}",
                trainee.getId(), trainee.getUsername(), active);
    }

    private void requireSameUser(String authenticatedUsername, String requestedUsername, String profileType) {
        if (!authenticatedUsername.equalsIgnoreCase(requestedUsername)) {
            throw new ValidationException("Authenticated " + profileType + " can only access own profile");
        }
    }

    private void validateAllTrainersFound(Set<String> requestedUsernames, List<Trainer> trainers) {
        Map<String, Trainer> foundByUsername = trainers.stream()
                .collect(Collectors.toMap(trainer -> trainer.getUsername().toLowerCase(), Function.identity()));
        requestedUsernames.stream()
                .filter(username -> !foundByUsername.containsKey(username.toLowerCase()))
                .findFirst()
                .ifPresent(missing -> {
                    throw new EntityNotFoundException("Trainer", missing);
                });
    }
}
