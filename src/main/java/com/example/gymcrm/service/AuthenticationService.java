package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.service.command.Credentials;

public interface AuthenticationService {
    boolean traineeCredentialsMatch(Credentials credentials);

    boolean trainerCredentialsMatch(Credentials credentials);

    Trainee authenticateTrainee(Credentials credentials);

    Trainer authenticateTrainer(Credentials credentials);
}
