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
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {
    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private TrainerServiceImpl service;

    @Test
    void createResolvesSpecializationGeneratesCredentialsAndSavesTrainer() {
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.of(yoga));
        when(usernameGenerator.generate("Alice", "Coach")).thenReturn("Alice.Coach");
        when(passwordGenerator.generate()).thenReturn("secret1234");
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainer trainer = service.create(new CreateTrainerCommand("Alice", "Coach", TrainingTypeName.YOGA, true));

        assertThat(trainer.getFirstName()).isEqualTo("Alice");
        assertThat(trainer.getLastName()).isEqualTo("Coach");
        assertThat(trainer.getUsername()).isEqualTo("Alice.Coach");
        assertThat(trainer.getPassword()).isEqualTo("secret1234");
        assertThat(trainer.getSpecialization()).isSameAs(yoga);
        verify(trainerRepository).save(trainer);
    }

    @Test
    void createRejectsUnknownSpecializationBeforeSaving() {
        when(trainingTypeRepository.findByName(TrainingTypeName.CARDIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateTrainerCommand(
                "Bob", "Trainer", TrainingTypeName.CARDIO, true)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainerRepository, never()).save(any());
    }

    @Test
    void findByUsernameReturnsAuthenticatedTrainerOnlyForOwnUsername() {
        Trainer trainer = trainer("Bob.Trainer", TrainingTypeName.CARDIO, true);
        Credentials credentials = credentials(trainer);
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);

        assertThat(service.findByUsername(credentials, "bob.trainer")).isSameAs(trainer);
        assertThatThrownBy(() -> service.findByUsername(credentials, "Other.Trainer"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateAndChangePasswordMutateTrainerButKeepSpecializationReadOnly() {
        Trainer trainer = trainer("Bob.Trainer", TrainingTypeName.CARDIO, true);
        TrainingType originalSpecialization = trainer.getSpecialization();
        Credentials credentials = credentials(trainer);
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);

        service.update(credentials, "Bob.Trainer", new UpdateTrainerCommand("Robert", "Trainer", false));
        service.changePassword(credentials, "newPassword");

        assertThat(trainer.getFirstName()).isEqualTo("Robert");
        assertThat(trainer.getSpecialization()).isSameAs(originalSpecialization);
        assertThat(trainer.isActive()).isFalse();
        assertThat(trainer.getPassword()).isEqualTo("newPassword");
        verify(trainingTypeRepository, never()).findByName(any());
    }

    @Test
    void statusChangesRejectRepeatedState() {
        Trainer trainer = trainer("Bob.Trainer", TrainingTypeName.CARDIO, true);
        Credentials credentials = credentials(trainer);
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);

        assertThat(service.deactivate(credentials)).isSameAs(trainer);
        assertThat(trainer.isActive()).isFalse();
        assertThatThrownBy(() -> service.deactivate(credentials))
                .isInstanceOf(ProfileStateException.class);
    }

    @Test
    void getTrainingsAuthenticatesOwnProfileAndDelegatesCriteria() {
        Trainer trainer = trainer("Coach.One", TrainingTypeName.YOGA, true);
        Trainee trainee = new Trainee(new User("Runner", "One", "Runner.One", "secret1234", true),
                LocalDate.of(2000, 1, 1), "Address");
        Training training = new Training(trainee, trainer, "Yoga", trainer.getSpecialization(),
                LocalDate.of(2026, 7, 2), 45);
        Credentials credentials = credentials(trainer);
        TrainerTrainingCriteria criteria = new TrainerTrainingCriteria(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Runner");
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);
        when(trainingRepository.findByTrainerUsername("Coach.One", criteria)).thenReturn(List.of(training));

        assertThat(service.getTrainings(credentials, "Coach.One", criteria)).containsExactly(training);
    }

    @Test
    void findAllDelegatesToRepository() {
        Trainer trainer = trainer("Coach.One", TrainingTypeName.YOGA, true);
        when(trainerRepository.findAll()).thenReturn(List.of(trainer));

        assertThat(service.findAll()).containsExactly(trainer);
    }

    private Credentials credentials(Trainer trainer) {
        return new Credentials(trainer.getUsername(), trainer.getPassword());
    }

    private Trainer trainer(String username, TrainingTypeName specialization, boolean active) {
        return new Trainer(new User("Bob", "Trainer", username, "secret1234", active),
                new TrainingType(specialization));
    }
}
