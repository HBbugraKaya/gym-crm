package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;

import java.util.Collection;
import java.util.List;

public interface TraineeService {
    CreatedAccount<Trainee> create(CreateTraineeCommand command);

    Trainee findByUsername(String username);

    Trainee update(String username, UpdateTraineeCommand command);

    void changePassword(String newPassword);

    Trainee activate();

    Trainee deactivate();

    void deleteByUsername(String username);

    List<Training> getTrainings(String traineeUsername, TraineeTrainingCriteria criteria);

    List<Trainer> getUnassignedTrainers(String traineeUsername);

    List<Trainer> updateTrainers(String traineeUsername, Collection<String> trainerUsernames);
}
