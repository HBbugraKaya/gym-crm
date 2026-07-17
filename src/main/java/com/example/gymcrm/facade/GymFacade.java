package com.example.gymcrm.facade;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.CreatedAccount;
import com.example.gymcrm.service.TraineeService;
import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class GymFacade {
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    public CreatedAccount<Trainee> createTrainee(CreateTraineeCommand command) {
        return traineeService.create(command);
    }

    public CreatedAccount<Trainer> createTrainer(CreateTrainerCommand command) {
        return trainerService.create(command);
    }

    public Trainee getTraineeProfile(String username) {
        return traineeService.findByUsername(username);
    }

    public Trainer getTrainerProfile(String username) {
        return trainerService.findByUsername(username);
    }

    public Trainee updateTrainee(String username, UpdateTraineeCommand command) {
        return traineeService.update(username, command);
    }

    public Trainer updateTrainer(String username, UpdateTrainerCommand command) {
        return trainerService.update(username, command);
    }

    public void deleteTrainee(String username) {
        traineeService.deleteByUsername(username);
    }

    public List<Training> getTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria) {
        return traineeService.getTrainings(traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria) {
        return trainerService.getTrainings(trainerUsername, criteria);
    }

    public Training addTraining(AddTrainingCommand command) {
        return trainingService.addTraining(command);
    }

    public List<Trainer> getUnassignedTrainers(String traineeUsername) {
        return traineeService.getUnassignedTrainers(traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(String traineeUsername, Collection<String> trainerUsernames) {
        return traineeService.updateTrainers(traineeUsername, trainerUsernames);
    }
}
