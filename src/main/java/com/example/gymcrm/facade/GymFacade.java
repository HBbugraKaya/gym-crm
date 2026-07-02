package com.example.gymcrm.facade;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.TraineeService;
import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
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
    private final AuthenticationService authenticationService;
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(AuthenticationService authenticationService,
                     TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.authenticationService = authenticationService;
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    public Trainee createTrainee(CreateTraineeCommand command) {
        return traineeService.create(command);
    }

    public Trainer createTrainer(CreateTrainerCommand command) {
        return trainerService.create(command);
    }

    public boolean traineeCredentialsMatch(Credentials credentials) {
        return authenticationService.traineeCredentialsMatch(credentials);
    }

    public boolean trainerCredentialsMatch(Credentials credentials) {
        return authenticationService.trainerCredentialsMatch(credentials);
    }

    public Trainee getTraineeProfile(Credentials credentials, String username) {
        return traineeService.findByUsername(credentials, username);
    }

    public Trainer getTrainerProfile(Credentials credentials, String username) {
        return trainerService.findByUsername(credentials, username);
    }

    public void changeTraineePassword(Credentials credentials, String newPassword) {
        traineeService.changePassword(credentials, newPassword);
    }

    public void changeTrainerPassword(Credentials credentials, String newPassword) {
        trainerService.changePassword(credentials, newPassword);
    }

    public Trainee updateTrainee(Credentials credentials, UpdateTraineeCommand command) {
        return traineeService.update(credentials, command);
    }

    public Trainer updateTrainer(Credentials credentials, UpdateTrainerCommand command) {
        return trainerService.update(credentials, command);
    }

    public Trainee activateTrainee(Credentials credentials) {
        return traineeService.activate(credentials);
    }

    public Trainee deactivateTrainee(Credentials credentials) {
        return traineeService.deactivate(credentials);
    }

    public Trainer activateTrainer(Credentials credentials) {
        return trainerService.activate(credentials);
    }

    public Trainer deactivateTrainer(Credentials credentials) {
        return trainerService.deactivate(credentials);
    }

    public void deleteTrainee(Credentials credentials, String username) {
        traineeService.deleteByUsername(credentials, username);
    }

    public List<Training> getTraineeTrainings(Credentials credentials, String traineeUsername,
                                              TraineeTrainingCriteria criteria) {
        return traineeService.getTrainings(credentials, traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(Credentials credentials, String trainerUsername,
                                              TrainerTrainingCriteria criteria) {
        return trainerService.getTrainings(credentials, trainerUsername, criteria);
    }

    public Training addTraining(Credentials trainerCredentials, AddTrainingCommand command) {
        return trainingService.addTraining(trainerCredentials, command);
    }

    public List<Trainer> getUnassignedTrainers(Credentials credentials, String traineeUsername) {
        return traineeService.getUnassignedTrainers(credentials, traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(Credentials credentials, String traineeUsername,
                                               Collection<String> trainerUsernames) {
        return traineeService.updateTrainers(credentials, traineeUsername, trainerUsernames);
    }

    public List<Trainee> findAllTrainees() {
        return traineeService.findAll();
    }

    public List<Trainer> findAllTrainers() {
        return trainerService.findAll();
    }

    public List<Training> findAllTrainings() {
        return trainingService.findAll();
    }
}
