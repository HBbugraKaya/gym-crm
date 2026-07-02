package com.example.gymcrm.facade;

import com.example.gymcrm.config.AppConfig;
import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GymFacadeHibernateIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private GymFacade facade;

    @BeforeEach
    void setUp() {
        System.setProperty("gym.db.url",
                "jdbc:h2:mem:gymcrm_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        facade = context.getBean(GymFacade.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        System.clearProperty("gym.db.url");
    }

    @Test
    void createsProfilesGeneratesCredentialsAndAuthenticates() {
        Trainee trainee = createTrainee("John", "Smith");
        Trainer trainer = createTrainer("Alice", "Coach", TrainingTypeName.YOGA);

        assertThat(trainee.getUsername()).isEqualTo("John.Smith");
        assertThat(trainee.getPassword()).hasSize(10);
        assertThat(trainer.getUsername()).isEqualTo("Alice.Coach");
        assertThat(trainer.getPassword()).hasSize(10);
        assertThat(facade.traineeCredentialsMatch(credentials(trainee))).isTrue();
        assertThat(facade.trainerCredentialsMatch(credentials(trainer))).isTrue();

        assertThat(facade.getTraineeProfile(credentials(trainee), trainee.getUsername()).getId())
                .isEqualTo(trainee.getId());
        assertThat(facade.getTrainerProfile(credentials(trainer), trainer.getUsername()).getSpecialization().getName())
                .isEqualTo(TrainingTypeName.YOGA);
    }

    @Test
    void duplicateNamesReceiveNumericUsernameSuffix() {
        Trainee trainee = createTrainee("John", "Smith");
        Trainer trainer = createTrainer("John", "Smith", TrainingTypeName.CARDIO);
        Trainee secondTrainee = createTrainee("John", "Smith");

        assertThat(trainee.getUsername()).isEqualTo("John.Smith");
        assertThat(trainer.getUsername()).isEqualTo("John.Smith1");
        assertThat(secondTrainee.getUsername()).isEqualTo("John.Smith2");
    }

    @Test
    void updatesPasswordProfileAndRejectsRepeatedStatusChange() {
        Trainee trainee = createTrainee("Jane", "Doe");
        Trainer trainer = createTrainer("Bob", "Trainer", TrainingTypeName.STRENGTH);

        Credentials traineeCredentials = credentials(trainee);
        Credentials trainerCredentials = credentials(trainer);

        Trainee inactiveTrainee = facade.deactivateTrainee(traineeCredentials);
        Trainer inactiveTrainer = facade.deactivateTrainer(trainerCredentials);

        assertThat(inactiveTrainee.isActive()).isFalse();
        assertThat(inactiveTrainer.isActive()).isFalse();
        assertThatThrownBy(() -> facade.deactivateTrainee(traineeCredentials))
                .isInstanceOf(ProfileStateException.class);
        assertThatThrownBy(() -> facade.deactivateTrainer(trainerCredentials))
                .isInstanceOf(ProfileStateException.class);

        facade.changeTraineePassword(traineeCredentials, "NewTrainee1");
        facade.changeTrainerPassword(trainerCredentials, "NewTrainer1");

        assertThat(facade.traineeCredentialsMatch(traineeCredentials)).isFalse();
        assertThat(facade.trainerCredentialsMatch(trainerCredentials)).isFalse();

        Credentials newTraineeCredentials = new Credentials(trainee.getUsername(), "NewTrainee1");
        Credentials newTrainerCredentials = new Credentials(trainer.getUsername(), "NewTrainer1");
        assertThat(facade.traineeCredentialsMatch(newTraineeCredentials)).isTrue();
        assertThat(facade.trainerCredentialsMatch(newTrainerCredentials)).isTrue();

        Trainee updatedTrainee = facade.updateTrainee(newTraineeCredentials, new UpdateTraineeCommand(
                "Janet", "Doe", LocalDate.of(1999, 5, 4), "Izmir", true));
        Trainer updatedTrainer = facade.updateTrainer(newTrainerCredentials, new UpdateTrainerCommand(
                "Robert", "Trainer", TrainingTypeName.FITNESS, true));

        assertThat(updatedTrainee.getFirstName()).isEqualTo("Janet");
        assertThat(updatedTrainee.getAddress()).isEqualTo("Izmir");
        assertThat(updatedTrainee.isActive()).isTrue();
        assertThat(updatedTrainer.getFirstName()).isEqualTo("Robert");
        assertThat(updatedTrainer.getSpecialization().getName()).isEqualTo(TrainingTypeName.FITNESS);
        assertThat(updatedTrainer.isActive()).isTrue();
    }

    @Test
    void addsTrainingFiltersListsAndUpdatesTrainerAssignments() {
        Trainee trainee = createTrainee("Mark", "Runner");
        Trainer yogaTrainer = createTrainer("Yara", "Yoga", TrainingTypeName.YOGA);
        Trainer cardioTrainer = createTrainer("Carl", "Cardio", TrainingTypeName.CARDIO);

        assertThat(facade.getUnassignedTrainers(credentials(trainee), trainee.getUsername()))
                .extracting(Trainer::getUsername)
                .containsExactly(yogaTrainer.getUsername(), cardioTrainer.getUsername());

        Training training = facade.addTraining(credentials(yogaTrainer), new AddTrainingCommand(
                trainee.getUsername(),
                yogaTrainer.getUsername(),
                "Evening yoga",
                TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2),
                45));

        assertThat(training.getId()).isNotNull();
        assertThat(facade.getTraineeTrainings(credentials(trainee), trainee.getUsername(),
                new TraineeTrainingCriteria(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
                        "Yara", TrainingTypeName.YOGA)))
                .extracting(Training::getName)
                .containsExactly("Evening yoga");
        assertThat(facade.getTrainerTrainings(credentials(yogaTrainer), yogaTrainer.getUsername(),
                new TrainerTrainingCriteria(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Mark")))
                .extracting(Training::getName)
                .containsExactly("Evening yoga");

        assertThat(facade.getUnassignedTrainers(credentials(trainee), trainee.getUsername()))
                .extracting(Trainer::getUsername)
                .containsExactly(cardioTrainer.getUsername());

        List<Trainer> assigned = facade.updateTraineeTrainers(
                credentials(trainee), trainee.getUsername(), List.of(cardioTrainer.getUsername()));
        assertThat(assigned).extracting(Trainer::getUsername).containsExactly(cardioTrainer.getUsername());
        assertThat(facade.getUnassignedTrainers(credentials(trainee), trainee.getUsername()))
                .extracting(Trainer::getUsername)
                .containsExactly(yogaTrainer.getUsername());
    }

    @Test
    void deleteTraineeCascadesRelatedTrainingsAndKeepsTrainer() {
        Trainee trainee = createTrainee("Delete", "Me");
        Trainer trainer = createTrainer("Keep", "Trainer", TrainingTypeName.STRETCHING);

        facade.addTraining(credentials(trainer), new AddTrainingCommand(
                trainee.getUsername(),
                trainer.getUsername(),
                "Stretching",
                TrainingTypeName.STRETCHING,
                LocalDate.of(2026, 8, 1),
                30));

        assertThat(facade.findAllTrainings()).hasSize(1);

        facade.deleteTrainee(credentials(trainee), trainee.getUsername());

        assertThat(facade.findAllTrainees()).isEmpty();
        assertThat(facade.findAllTrainings()).isEmpty();
        assertThat(facade.findAllTrainers()).extracting(Trainer::getUsername)
                .containsExactly(trainer.getUsername());
    }

    @Test
    void rejectsUnauthorizedAccessAndRollsBackFailedTraining() {
        Trainee firstTrainee = createTrainee("First", "User");
        Trainee secondTrainee = createTrainee("Second", "User");
        Trainer trainer = createTrainer("Rollback", "Trainer", TrainingTypeName.CARDIO);

        assertThatThrownBy(() -> facade.getTraineeProfile(credentials(firstTrainee), secondTrainee.getUsername()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> facade.getTrainerProfile(new Credentials(trainer.getUsername(), "bad-password"),
                trainer.getUsername()))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> facade.addTraining(credentials(trainer), new AddTrainingCommand(
                firstTrainee.getUsername(),
                "Some.OtherTrainer",
                "Invalid",
                TrainingTypeName.CARDIO,
                LocalDate.of(2026, 9, 1),
                40)))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> facade.addTraining(credentials(trainer), new AddTrainingCommand(
                firstTrainee.getUsername(),
                trainer.getUsername(),
                "Invalid duration",
                TrainingTypeName.CARDIO,
                LocalDate.of(2026, 9, 1),
                0)))
                .isInstanceOf(ValidationException.class);

        assertThat(facade.findAllTrainings()).isEmpty();
        assertThat(facade.getUnassignedTrainers(credentials(firstTrainee), firstTrainee.getUsername()))
                .extracting(Trainer::getUsername)
                .containsExactly(trainer.getUsername());
    }

    private Trainee createTrainee(String firstName, String lastName) {
        return facade.createTrainee(new CreateTraineeCommand(
                firstName, lastName, LocalDate.of(2000, 1, 1), "Address", true));
    }

    private Trainer createTrainer(String firstName, String lastName, TrainingTypeName specialization) {
        return facade.createTrainer(new CreateTrainerCommand(firstName, lastName, specialization, true));
    }

    private Credentials credentials(Trainee trainee) {
        return new Credentials(trainee.getUsername(), trainee.getPassword());
    }

    private Credentials credentials(Trainer trainer) {
        return new Credentials(trainer.getUsername(), trainer.getPassword());
    }
}
