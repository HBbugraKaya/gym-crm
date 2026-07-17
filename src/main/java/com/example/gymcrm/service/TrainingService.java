package com.example.gymcrm.service;

import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.command.AddTrainingCommand;

public interface TrainingService {
    Training addTraining(AddTrainingCommand command);
}
