package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.security.CurrentUser;
import com.example.gymcrm.service.command.AddTrainingCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {
    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private GymCrmMetrics metrics;

    @InjectMocks
    private TrainingService service;

    @Test
    void addTrainingAuthenticatesTrainerLoadsReferencesAssignsTrainerAndSavesTraining() {
        Trainer trainer = trainer("Coach.One");
        Trainee trainee = trainee("Runner.One");
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        AddTrainingCommand command = new AddTrainingCommand(
                "Runner.One", "Coach.One", "Evening yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45);
        when(currentUser.requireTrainer()).thenReturn(trainer);
        when(traineeRepository.findByUsername("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.of(yoga));
        when(trainingRepository.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training saved = service.addTraining(command);

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
        verify(metrics).recordTrainingCreated();
    }

    @Test
    void addTrainingCanDeriveTrainingTypeFromTrainerSpecialization() {
        Trainer trainer = trainer("Coach.One");
        Trainee trainee = trainee("Runner.One");
        AddTrainingCommand command = new AddTrainingCommand(
                "Runner.One", "Coach.One", "Morning yoga",
                LocalDate.of(2026, 7, 3), 30);
        when(currentUser.requireTrainer()).thenReturn(trainer);
        when(traineeRepository.findByUsername("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingRepository.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training saved = service.addTraining(command);

        assertThat(saved.getTrainingType()).isSameAs(trainer.getSpecialization());
        verify(trainingTypeRepository, never()).findByName(any());
    }

    @Test
    void addTrainingRejectsTrainerUsernameMismatchBeforeRepositoryLookup() {
        Trainer trainer = trainer("Coach.One");
        when(currentUser.requireTrainer()).thenReturn(trainer);

        assertThatThrownBy(() -> service.addTraining(new AddTrainingCommand(
                "Runner.One", "Other.Coach", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(ValidationException.class);
        verify(traineeRepository, never()).findByUsername(any());
        verify(trainingRepository, never()).save(any());
    }

    @Test
    void addTrainingRejectsMissingTraineeOrTrainingType() {
        Trainer trainer = trainer("Coach.One");
        when(currentUser.requireTrainer()).thenReturn(trainer);
        when(traineeRepository.findByUsername("Missing.Runner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(new AddTrainingCommand(
                "Missing.Runner", "Coach.One", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(EntityNotFoundException.class);

        Trainee trainee = trainee("Runner.One");
        when(traineeRepository.findByUsername("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(new AddTrainingCommand(
                "Runner.One", "Coach.One", "Yoga", TrainingTypeName.YOGA,
                LocalDate.of(2026, 7, 2), 45)))
                .isInstanceOf(EntityNotFoundException.class);
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
