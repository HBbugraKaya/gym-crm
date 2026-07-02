package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;

import java.util.Collection;
import java.util.List;

public interface TraineeService {
    Trainee create(CreateTraineeCommand command);

    Trainee findByUsername(Credentials credentials, String username);

    Trainee update(Credentials credentials, UpdateTraineeCommand command);

    void changePassword(Credentials credentials, String newPassword);

    Trainee activate(Credentials credentials);

    Trainee deactivate(Credentials credentials);

    void deleteByUsername(Credentials credentials, String username);

    List<Training> getTrainings(Credentials credentials, String traineeUsername, TraineeTrainingCriteria criteria);

    List<Trainer> getUnassignedTrainers(Credentials credentials, String traineeUsername);

    List<Trainer> updateTrainers(Credentials credentials, String traineeUsername, Collection<String> trainerUsernames);

    List<Trainee> findAll();
}
