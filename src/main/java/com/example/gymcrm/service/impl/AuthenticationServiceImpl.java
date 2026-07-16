package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.command.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;

    public AuthenticationServiceImpl(TraineeRepository traineeRepository, TrainerRepository trainerRepository) {
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean traineeCredentialsMatch(Credentials credentials) {
        return traineeRepository.findByUsername(credentials.username())
                .filter(trainee -> trainee.getPassword().equals(credentials.password()))
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean trainerCredentialsMatch(Credentials credentials) {
        return trainerRepository.findByUsername(credentials.username())
                .filter(trainer -> trainer.getPassword().equals(credentials.password()))
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Trainee authenticateTrainee(Credentials credentials) {
        return traineeRepository.findByUsername(credentials.username())
                .filter(trainee -> trainee.getPassword().equals(credentials.password()))
                .map(trainee -> {
                    LOGGER.debug("Trainee authenticated username={}", trainee.getUsername());
                    return trainee;
                })
                .orElseThrow(() -> {
                    LOGGER.warn("Trainee authentication failed username={}", credentials.username());
                    return new AuthenticationException("Trainee");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer authenticateTrainer(Credentials credentials) {
        return trainerRepository.findByUsername(credentials.username())
                .filter(trainer -> trainer.getPassword().equals(credentials.password()))
                .map(trainer -> {
                    LOGGER.debug("Trainer authenticated username={}", trainer.getUsername());
                    return trainer;
                })
                .orElseThrow(() -> {
                    LOGGER.warn("Trainer authentication failed username={}", credentials.username());
                    return new AuthenticationException("Trainer");
                });
    }
}
