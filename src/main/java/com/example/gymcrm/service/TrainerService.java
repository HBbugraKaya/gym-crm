package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;

import java.util.List;

public interface TrainerService {
    CreatedAccount<Trainer> create(CreateTrainerCommand command);

    Trainer findByUsername(String username);

    Trainer update(String username, UpdateTrainerCommand command);

    void changePassword(String newPassword);

    Trainer activate();

    Trainer deactivate();

    List<Training> getTrainings(String trainerUsername, TrainerTrainingCriteria criteria);
}
