package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.generator.PasswordGenerator;
import com.example.gymcrm.generator.UsernameGenerator;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {
    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private TraineeServiceImpl service;

    @Test
    void createGeneratesCredentialsAndSavesTrainee() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("secret1234");
        when(traineeRepository.save(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainee trainee = service.create(new CreateTraineeCommand(
                "John", "Smith", LocalDate.of(2000, 1, 1), "Address", true));

        assertThat(trainee.getFirstName()).isEqualTo("John");
        assertThat(trainee.getLastName()).isEqualTo("Smith");
        assertThat(trainee.getUsername()).isEqualTo("John.Smith");
        assertThat(trainee.getPassword()).isEqualTo("secret1234");
        assertThat(trainee.getAddress()).isEqualTo("Address");
        verify(traineeRepository).save(trainee);
    }

    @Test
    void findByUsernameReturnsAuthenticatedTraineeOnlyForOwnUsername() {
        Trainee trainee = trainee("John.Smith", true);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);

        assertThat(service.findByUsername(credentials, "john.smith")).isSameAs(trainee);
        assertThatThrownBy(() -> service.findByUsername(credentials, "Other.User"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateAndChangePasswordMutateAuthenticatedTrainee() {
        Trainee trainee = trainee("Jane.Doe", true);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);

        service.update(credentials, "Jane.Doe", new UpdateTraineeCommand(
                "Janet", "Doe", LocalDate.of(1999, 5, 4), "Izmir", false));
        service.changePassword(credentials, "newPassword");

        assertThat(trainee.getFirstName()).isEqualTo("Janet");
        assertThat(trainee.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 5, 4));
        assertThat(trainee.getAddress()).isEqualTo("Izmir");
        assertThat(trainee.isActive()).isFalse();
        assertThat(trainee.getPassword()).isEqualTo("newPassword");
    }

    @Test
    void statusChangesRejectRepeatedState() {
        Trainee trainee = trainee("Jane.Doe", true);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);

        assertThat(service.deactivate(credentials)).isSameAs(trainee);
        assertThat(trainee.isActive()).isFalse();
        assertThatThrownBy(() -> service.deactivate(credentials))
                .isInstanceOf(ProfileStateException.class);
    }

    @Test
    void activateChangesInactiveTraineeAndRejectsRepeatedActiveState() {
        Trainee trainee = trainee("Jane.Doe", false);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);

        assertThat(service.activate(credentials)).isSameAs(trainee);
        assertThat(trainee.isActive()).isTrue();
        assertThatThrownBy(() -> service.activate(credentials))
                .isInstanceOf(ProfileStateException.class);
    }

    @Test
    void deleteClearsAssignmentsAndDelegatesToRepository() {
        Trainee trainee = trainee("Delete.Me", true);
        Trainer trainer = trainer("Keep.Trainer", 11L);
        trainee.assignTrainer(trainer);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);

        service.deleteByUsername(credentials, "Delete.Me");

        assertThat(trainee.getTrainers()).isEmpty();
        assertThat(trainer.getTrainees()).isEmpty();
        verify(traineeRepository).delete(trainee);
    }

    @Test
    void getTrainingsAuthenticatesOwnProfileAndDelegatesCriteria() {
        Trainee trainee = trainee("Runner.One", true);
        Credentials credentials = credentials(trainee);
        TraineeTrainingCriteria criteria = new TraineeTrainingCriteria(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Coach", TrainingTypeName.YOGA);
        Training training = training(trainee, trainer("Coach.One", 12L));
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);
        when(trainingRepository.findByTraineeUsername("Runner.One", criteria)).thenReturn(List.of(training));

        assertThat(service.getTrainings(credentials, "Runner.One", criteria)).containsExactly(training);
    }

    @Test
    void getUnassignedTrainersFiltersAssignedAndInactiveTrainers() {
        Trainee trainee = trainee("Runner.One", true);
        Trainer assigned = trainer("Assigned.Trainer", 21L);
        Trainer unassigned = trainer("Free.Trainer", 22L);
        Trainer inactive = new Trainer(
                new User("Inactive", "Trainer", "Inactive.Trainer", "secret1234", false),
                new TrainingType(TrainingTypeName.YOGA));
        setId(inactive, 23L);
        trainee.assignTrainer(assigned);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAll()).thenReturn(List.of(assigned, unassigned, inactive));

        assertThat(service.getUnassignedTrainers(credentials, "Runner.One")).containsExactly(unassigned);
    }

    @Test
    void updateTrainersReplacesAssignmentsAndRejectsMissingTrainer() {
        Trainee trainee = trainee("Runner.One", true);
        Trainer trainer = trainer("New.Trainer", 31L);
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAllByUsernames(argThat(usernames ->
                usernames.containsAll(List.of("New.Trainer", "Missing.Trainer")) && usernames.size() == 2)))
                .thenReturn(List.of(trainer));

        assertThatThrownBy(() -> service.updateTrainers(credentials, "Runner.One",
                List.of("New.Trainer", "Missing.Trainer")))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainerRepository).findAllByUsernames(argThat(usernames ->
                usernames.containsAll(List.of("New.Trainer", "Missing.Trainer")) && usernames.size() == 2));
        assertThat(trainee.getTrainers()).isEmpty();
    }

    @Test
    void updateTrainersAcceptsEmptyListAndClearsAssignments() {
        Trainee trainee = trainee("Runner.One", true);
        trainee.assignTrainer(trainer("Old.Trainer", 41L));
        Credentials credentials = credentials(trainee);
        when(authenticationService.authenticateTrainee(credentials)).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAllByUsernames(anyCollection())).thenReturn(List.of());

        assertThat(service.updateTrainers(credentials, "Runner.One", null)).isEmpty();
        assertThat(trainee.getTrainers()).isEmpty();
    }

    @Test
    void findAllDelegatesToRepository() {
        Trainee trainee = trainee("Jane.Doe", true);
        when(traineeRepository.findAll()).thenReturn(List.of(trainee));

        assertThat(service.findAll()).containsExactly(trainee);
    }

    private Credentials credentials(Trainee trainee) {
        return new Credentials(trainee.getUsername(), trainee.getPassword());
    }

    private Trainee trainee(String username, boolean active) {
        return new Trainee(new User("John", "Smith", username, "secret1234", active),
                LocalDate.of(2000, 1, 1), "Address");
    }

    private Trainer trainer(String username, long id) {
        Trainer trainer = new Trainer(new User("Coach", "One", username, "secret1234", true),
                new TrainingType(TrainingTypeName.YOGA));
        setId(trainer, id);
        return trainer;
    }

    private Training training(Trainee trainee, Trainer trainer) {
        return new Training(trainee, trainer, "Yoga", trainer.getSpecialization(),
                LocalDate.of(2026, 7, 2), 45);
    }

    private void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
