package com.example.gymcrm.service;

import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;

import java.util.List;

public interface TrainingService {
    Training addTraining(Credentials trainerCredentials, AddTrainingCommand command);

    List<Training> findAll();
}
