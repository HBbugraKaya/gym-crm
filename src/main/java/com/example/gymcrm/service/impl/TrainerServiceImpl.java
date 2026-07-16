package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.generator.PasswordGenerator;
import com.example.gymcrm.generator.UsernameGenerator;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainerServiceImpl implements TrainerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerRepository trainerRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final TrainingRepository trainingRepository;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final AuthenticationService authenticationService;

    public TrainerServiceImpl(TrainerRepository trainerRepository,
                              TrainingTypeRepository trainingTypeRepository,
                              TrainingRepository trainingRepository,
                              UsernameGenerator usernameGenerator,
                              PasswordGenerator passwordGenerator,
                              AuthenticationService authenticationService) {
        this.trainerRepository = trainerRepository;
        this.trainingTypeRepository = trainingTypeRepository;
        this.trainingRepository = trainingRepository;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.authenticationService = authenticationService;
    }

    @Override
    @Transactional
    public Trainer create(CreateTrainerCommand command) {
        TrainingType specialization = findTrainingType(command.specialization());
        String username = usernameGenerator.generate(command.firstName(), command.lastName());
        String password = passwordGenerator.generate();

        Trainer trainer = new Trainer(
                new User(command.firstName(), command.lastName(), username, password, command.active()),
                specialization);
        Trainer saved = trainerRepository.save(trainer);
        LOGGER.info("Created trainer id={} username={} specialization={}",
                saved.getId(), saved.getUsername(), specialization.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer findByUsername(Credentials credentials, String username) {
        Trainer authenticated = authenticationService.authenticateTrainer(credentials);
        requireSameUser(authenticated.getUsername(), username, "trainer");
        authenticated.getTrainees().size();
        LOGGER.debug("Selected trainer profile username={}", authenticated.getUsername());
        return authenticated;
    }

    @Override
    @Transactional
    public Trainer update(Credentials credentials, String username, UpdateTrainerCommand command) {
        Trainer trainer = authenticationService.authenticateTrainer(credentials);
        requireSameUser(trainer.getUsername(), username, "trainer");
        trainer.updateProfile(command.firstName(), command.lastName(), command.active());
        trainer.getTrainees().size();
        LOGGER.info("Updated trainer id={} username={} specialization={}",
                trainer.getId(), trainer.getUsername(), trainer.getSpecialization().getName());
        return trainer;
    }

    @Override
    @Transactional
    public void changePassword(Credentials credentials, String newPassword) {
        Trainer trainer = authenticationService.authenticateTrainer(credentials);
        trainer.changePassword(newPassword);
        LOGGER.info("Changed trainer password id={} username={}", trainer.getId(), trainer.getUsername());
    }

    @Override
    @Transactional
    public Trainer activate(Credentials credentials) {
        Trainer trainer = authenticationService.authenticateTrainer(credentials);
        changeStatus(trainer, true);
        return trainer;
    }

    @Override
    @Transactional
    public Trainer deactivate(Credentials credentials) {
        Trainer trainer = authenticationService.authenticateTrainer(credentials);
        changeStatus(trainer, false);
        return trainer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTrainings(Credentials credentials, String trainerUsername, TrainerTrainingCriteria criteria) {
        Trainer trainer = authenticationService.authenticateTrainer(credentials);
        requireSameUser(trainer.getUsername(), trainerUsername, "trainer");
        List<Training> trainings = trainingRepository.findByTrainerUsername(trainerUsername, criteria);
        LOGGER.debug("Loaded trainer trainings username={} count={}", trainerUsername, trainings.size());
        return trainings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> findAll() {
        return trainerRepository.findAll();
    }

    private TrainingType findTrainingType(TrainingTypeName name) {
        return trainingTypeRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("TrainingType", name.name()));
    }

    private void changeStatus(Trainer trainer, boolean active) {
        if (trainer.isActive() == active) {
            LOGGER.warn("Trainer status change rejected id={} username={} active={}",
                    trainer.getId(), trainer.getUsername(), active);
            throw new ProfileStateException("Trainer is already " + (active ? "active" : "inactive"));
        }
        trainer.setActive(active);
        LOGGER.info("Changed trainer status id={} username={} active={}",
                trainer.getId(), trainer.getUsername(), active);
    }

    private void requireSameUser(String authenticatedUsername, String requestedUsername, String profileType) {
        if (!authenticatedUsername.equalsIgnoreCase(requestedUsername)) {
            throw new ValidationException("Authenticated " + profileType + " can only access own profile");
        }
    }
}
