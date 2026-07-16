package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;

import java.util.List;

public interface TrainerService {
    Trainer create(CreateTrainerCommand command);

    Trainer findByUsername(Credentials credentials, String username);

    Trainer update(Credentials credentials, String username, UpdateTrainerCommand command);

    void changePassword(Credentials credentials, String newPassword);

    Trainer activate(Credentials credentials);

    Trainer deactivate(Credentials credentials);

    List<Training> getTrainings(Credentials credentials, String trainerUsername, TrainerTrainingCriteria criteria);

    List<Trainer> findAll();
}
