package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.integration.jms.TrainerWorkloadPublisher;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {
    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private GymCrmMetrics metrics;

    @Mock
    private TrainerWorkloadPublisher trainerWorkloadPublisher;

    @InjectMocks
    private TrainingService service;

    @Test
    void addTrainingAuthenticatesTrainerLoadsReferencesAssignsTrainerAndSavesTraining() {
        Trainer trainer = trainer("Coach.One");
        Trainee trainee = trainee("Runner.One");
        when(trainerRepository.findByUserUsernameIgnoreCase("Coach.One")).thenReturn(Optional.of(trainer));
        when(traineeRepository.findByUserUsernameIgnoreCase("Runner.One")).thenReturn(Optional.of(trainee));
        when(trainingRepository.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training saved = service.addTraining(
                "Runner.One", "Coach.One", "Evening yoga", LocalDate.of(2026, 7, 2), 45);

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingRepository).save(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getTrainee()).isSameAs(trainee);
        assertThat(saved.getTrainer()).isSameAs(trainer);
        assertThat(saved.getName()).isEqualTo("Evening yoga");
        assertThat(saved.getTrainingType()).isSameAs(trainer.getSpecialization());
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(saved.getDurationMinutes()).isEqualTo(45);
        assertThat(trainee.getTrainers()).containsExactly(trainer);
        assertThat(trainer.getTrainees()).containsExactly(trainee);
        verify(metrics).recordTrainingCreated();
        verify(trainerWorkloadPublisher).publish(any());
    }

    @Test
    void addTrainingRejectsMissingTrainerBeforeTraineeLookup() {
        when(trainerRepository.findByUserUsernameIgnoreCase("Other.Coach")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(
                "Runner.One", "Other.Coach", "Yoga", LocalDate.of(2026, 7, 2), 45))
                .isInstanceOf(EntityNotFoundException.class);
        verify(traineeRepository, never()).findByUserUsernameIgnoreCase(any());
        verify(trainingRepository, never()).save(any());
    }

    @Test
    void addTrainingRejectsMissingTrainee() {
        Trainer trainer = trainer("Coach.One");
        when(trainerRepository.findByUserUsernameIgnoreCase("Coach.One")).thenReturn(Optional.of(trainer));
        when(traineeRepository.findByUserUsernameIgnoreCase("Missing.Runner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTraining(
                "Missing.Runner", "Coach.One", "Yoga", LocalDate.of(2026, 7, 2), 45))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteTrainingSynchronizesDeleteAndRemovesTrainingForOwner() {
        Trainer trainer = trainer("Coach.One");
        Trainee trainee = trainee("Runner.One");
        Training training = new Training(
                trainee,
                trainer,
                "Morning yoga",
                trainer.getSpecialization(),
                LocalDate.of(2026, 7, 2),
                45);
        when(trainingRepository.findById(42L)).thenReturn(Optional.of(training));
        org.springframework.security.core.Authentication authentication =
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("coach.one");
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        try {
            service.deleteTraining(42L);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        verify(trainingRepository).delete(training);
        verify(trainerWorkloadPublisher).publish(any());
    }

    @Test
    void deleteTrainingRejectsTrainingOwnedByAnotherTrainer() {
        Trainer trainer = trainer("Coach.One");
        Training training = new Training(
                trainee("Runner.One"),
                trainer,
                "Morning yoga",
                trainer.getSpecialization(),
                LocalDate.of(2026, 7, 2),
                45);
        when(trainingRepository.findById(42L)).thenReturn(Optional.of(training));
        org.springframework.security.core.Authentication authentication =
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.getName()).thenReturn("Other.Coach");
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        try {
            assertThatThrownBy(() -> service.deleteTraining(42L))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        verifyNoInteractions(trainerWorkloadPublisher);
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
