package com.example.gymcrm.application;

import com.example.gymcrm.config.AppConfig;
import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class GymCrmApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(GymCrmApplication.class);

    private GymCrmApplication() {
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);
            LOGGER.info("Gym CRM started: trainees={}, trainers={}, trainings={}",
                    facade.findAllTrainees().size(),
                    facade.findAllTrainers().size(),
                    facade.findAllTrainings().size());

            if (Arrays.asList(args).contains("--demo")) {
                runDemo(facade);
            }
        }
    }

    private static void runDemo(GymFacade facade) {
        LOGGER.info("Demo scenario started");

        Trainee trainee = facade.createTrainee(new CreateTraineeCommand(
                "John", "Smith", LocalDate.of(2001, 1, 1), "Istanbul", true));
        Trainer trainer = facade.createTrainer(new CreateTrainerCommand(
                "Alice", "Coach", TrainingTypeName.YOGA, true));

        Credentials traineeCredentials = new Credentials(trainee.getUsername(), trainee.getPassword());
        Credentials trainerCredentials = new Credentials(trainer.getUsername(), trainer.getPassword());

        facade.addTraining(trainerCredentials, new AddTrainingCommand(
                trainee.getUsername(),
                trainer.getUsername(),
                "Morning yoga",
                TrainingTypeName.YOGA,
                LocalDate.now(),
                60));

        facade.updateTrainee(traineeCredentials, new UpdateTraineeCommand(
                "John", "Smith", LocalDate.of(2001, 1, 1), "Updated address", true));
        List<?> trainings = facade.getTraineeTrainings(
                traineeCredentials,
                trainee.getUsername(),
                new TraineeTrainingCriteria(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                        "Alice", TrainingTypeName.YOGA));

        LOGGER.info("Demo completed: traineeUsername={} trainerUsername={} trainingMatches={}",
                trainee.getUsername(), trainer.getUsername(), trainings.size());
    }
}
