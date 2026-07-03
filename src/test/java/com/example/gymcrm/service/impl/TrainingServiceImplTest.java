package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.service.AuthenticationService;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class TrainingServiceImplTest {
    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private TrainingServiceImpl service;

    @Test
    void addTrainingAuthenticatesTrainerLoadsReferencesAssignsTrainerAndSavesTraining() {
        Trainer trainer = trainer("Coach.One");
        Trainee trainee = trainee("Runner.One");
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        Credentials credentials = new Credentials("Coach.One", "secret1234");
        AddTrainingCommand command = new AddTrainingCommand(
                "Runner.One", "Coach.One", "Evening yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45);
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);
        when(traineeRepository.findByUsername("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.of(yoga));
        when(trainingRepository.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training saved = service.addTraining(credentials, command);

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingRepository).save(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getTrainee()).isSameAs(trainee);
        assertThat(saved.getTrainer()).isSameAs(trainer);
        assertThat(saved.getName()).isEqualTo("Evening yoga");
        assertThat(saved.getTrainingType()).isSameAs(yoga);
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(saved.getDurationMinutes()).isEqualTo(45);
        assertThat(trainee.getTrainers()).containsExactly(trainer);
        assertThat(trainer.getTrainees()).containsExactly(trainee);
    }

    @Test
    void addTrainingRejectsTrainerUsernameMismatchBeforeRepositoryLookup() {
        Trainer trainer = trainer("Coach.One");
        Credentials credentials = new Credentials("Coach.One", "secret1234");
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);

        assertThatThrownBy(() -> service.addTraining(credentials, new AddTrainingCommand(
                "Runner.One", "Other.Coach", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(ValidationException.class);
        verify(traineeRepository, never()).findByUsername(any());
        verify(trainingRepository, never()).save(any());
    }

    @Test
    void addTrainingRejectsMissingTraineeOrTrainingType() {
        Trainer trainer = trainer("Coach.One");
        Credentials credentials = new Credentials("Coach.One", "secret1234");
        when(authenticationService.authenticateTrainer(credentials)).thenReturn(trainer);
        when(traineeRepository.findByUsername("Missing.Runner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(credentials, new AddTrainingCommand(
                "Missing.Runner", "Coach.One", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(EntityNotFoundException.class);

        Trainee trainee = trainee("Runner.One");
        when(traineeRepository.findByUsername("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(credentials, new AddTrainingCommand(
                "Runner.One", "Coach.One", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addTrainingRejectsInvalidDurationBeforeSaving() {
        Credentials credentials = new Credentials("Coach.One", "secret1234");

        assertThatThrownBy(() -> service.addTraining(credentials, new AddTrainingCommand(
                "Runner.One", "Coach.One", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 0)))
                .isInstanceOf(ValidationException.class);
        verify(authenticationService, never()).authenticateTrainer(any());
        verify(traineeRepository, never()).findByUsername(any());
        verify(trainingTypeRepository, never()).findByName(any());
        verify(trainingRepository, never()).save(any());
    }

    @Test
    void addTrainingRejectsBlankRequiredFieldsBeforeAuthenticationAndLookup() {
        Credentials credentials = new Credentials("Coach.One", "secret1234");

        assertThatThrownBy(() -> service.addTraining(credentials, new AddTrainingCommand(
                "Runner.One", "Coach.One", " ", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(ValidationException.class);

        verify(authenticationService, never()).authenticateTrainer(any());
        verify(traineeRepository, never()).findByUsername(any());
        verify(trainingTypeRepository, never()).findByName(any());
        verify(trainingRepository, never()).save(any());
    }

    @Test
    void findAllDelegatesToRepository() {
        Training training = new Training(trainee("Runner.One"), trainer("Coach.One"), "Yoga",
                new TrainingType(TrainingTypeName.YOGA), LocalDate.of(2026, 7, 2), 45);
        when(trainingRepository.findAll()).thenReturn(List.of(training));

        assertThat(service.findAll()).containsExactly(training);
    }

    private Trainee trainee(String username) {
        return new Trainee(new User("Runner", "One", username, "secret1234", true),
                LocalDate.of(2000, 1, 1), "Address");
    }

    private Trainer trainer(String username) {
        return new Trainer(new User("Coach", "One", username, "secret1234", true),
                new TrainingType(TrainingTypeName.YOGA));
    }
}
