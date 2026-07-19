package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.generator.SecurePasswordGenerator;
import com.example.gymcrm.generator.UniqueUsernameGenerator;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.security.CurrentUser;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {
    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private UniqueUsernameGenerator usernameGenerator;

    @Mock
    private SecurePasswordGenerator passwordGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private GymCrmMetrics metrics;

    @InjectMocks
    private TraineeService service;

    @Test
    void createGeneratesCredentialsEncodesPasswordAndSavesTrainee() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("secret1234");
        when(passwordEncoder.encode("secret1234")).thenReturn("encoded-secret1234");
        when(traineeRepository.save(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateTraineeCommand(
                "John", "Smith", LocalDate.of(2000, 1, 1), "Address", true));

        assertThat(created.rawPassword()).isEqualTo("secret1234");
        assertThat(created.profile().getFirstName()).isEqualTo("John");
        assertThat(created.profile().getLastName()).isEqualTo("Smith");
        assertThat(created.profile().getUsername()).isEqualTo("John.Smith");
        assertThat(created.profile().getPassword()).isEqualTo("encoded-secret1234");
        assertThat(created.profile().getAddress()).isEqualTo("Address");
        verify(traineeRepository).save(created.profile());
        verify(metrics).recordTraineeRegistration();
    }

    @Test
    void findByUsernameReturnsAuthenticatedTraineeOnlyForOwnUsername() {
        Trainee trainee = trainee("John.Smith", true);
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("John.Smith")).thenReturn(Optional.of(trainee));

        assertThat(service.findByUsername("john.smith")).isSameAs(trainee);
        assertThatThrownBy(() -> service.findByUsername("Other.User"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateMutatesAuthenticatedTrainee() {
        Trainee trainee = trainee("Jane.Doe", true);
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Jane.Doe")).thenReturn(Optional.of(trainee));

        service.update("Jane.Doe", new UpdateTraineeCommand(
                "Janet", "Doe", LocalDate.of(1999, 5, 4), "Izmir", false));

        assertThat(trainee.getFirstName()).isEqualTo("Janet");
        assertThat(trainee.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 5, 4));
        assertThat(trainee.getAddress()).isEqualTo("Izmir");
        assertThat(trainee.isActive()).isFalse();
    }

    @Test
    void deleteClearsAssignmentsAndDelegatesToRepository() {
        Trainee trainee = trainee("Delete.Me", true);
        Trainer trainer = trainer("Keep.Trainer", 11L);
        trainee.assignTrainer(trainer);
        when(currentUser.requireTrainee()).thenReturn(trainee);

        service.deleteByUsername("Delete.Me");

        assertThat(trainee.getTrainers()).isEmpty();
        assertThat(trainer.getTrainees()).isEmpty();
        verify(traineeRepository).delete(trainee);
    }

    @Test
    void getTrainingsAuthenticatesOwnProfileAndDelegatesCriteria() {
        Trainee trainee = trainee("Runner.One", true);
        TraineeTrainingCriteria criteria = new TraineeTrainingCriteria(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Coach", TrainingTypeName.YOGA);
        Training training = training(trainee, trainer("Coach.One", 12L));
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(trainingRepository.findByTraineeUsername("Runner.One", criteria)).thenReturn(List.of(training));

        assertThat(service.getTrainings("Runner.One", criteria)).containsExactly(training);
    }

    @Test
    void getUnassignedTrainersDelegatesToRepository() {
        Trainee trainee = trainee("Runner.One", true);
        Trainer unassigned = trainer("Free.Trainer", 22L);
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(trainerRepository.findUnassignedActiveTrainers("Runner.One")).thenReturn(List.of(unassigned));

        assertThat(service.getUnassignedTrainers("Runner.One")).containsExactly(unassigned);
    }

    @Test
    void updateTrainersReplacesAssignmentsAndRejectsMissingTrainer() {
        Trainee trainee = trainee("Runner.One", true);
        Trainer trainer = trainer("New.Trainer", 31L);
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAllByUsernames(argThat(usernames ->
                usernames.containsAll(List.of("New.Trainer", "Missing.Trainer")) && usernames.size() == 2)))
                .thenReturn(List.of(trainer));

        assertThatThrownBy(() -> service.updateTrainers("Runner.One",
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
        when(currentUser.requireTrainee()).thenReturn(trainee);
        when(traineeRepository.findByUsernameWithTrainers("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAllByUsernames(anyCollection())).thenReturn(List.of());

        assertThat(service.updateTrainers("Runner.One", null)).isEmpty();
        assertThat(trainee.getTrainers()).isEmpty();
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
