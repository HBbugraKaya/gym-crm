package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.generator.SecurePasswordGenerator;
import com.example.gymcrm.generator.UniqueUsernameGenerator;
import com.example.gymcrm.observability.GymCrmMetrics;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class TrainerServiceTest {
    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private UniqueUsernameGenerator usernameGenerator;

    @Mock
    private SecurePasswordGenerator passwordGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GymCrmMetrics metrics;

    @InjectMocks
    private TrainerService service;

    @Test
    void createResolvesSpecializationGeneratesCredentialsAndSavesTrainer() {
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        when(trainingTypeRepository.findByName(TrainingTypeName.YOGA)).thenReturn(Optional.of(yoga));
        when(usernameGenerator.generate("Alice", "Coach")).thenReturn("Alice.Coach");
        when(passwordGenerator.generate()).thenReturn("secret1234");
        when(passwordEncoder.encode("secret1234")).thenReturn("encoded-secret1234");
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create("Alice", "Coach", TrainingTypeName.YOGA);

        assertThat(created.rawPassword()).isEqualTo("secret1234");
        assertThat(created.profile().getFirstName()).isEqualTo("Alice");
        assertThat(created.profile().getLastName()).isEqualTo("Coach");
        assertThat(created.profile().getUsername()).isEqualTo("Alice.Coach");
        assertThat(created.profile().getPassword()).isEqualTo("encoded-secret1234");
        assertThat(created.profile().getSpecialization()).isSameAs(yoga);
        assertThat(created.profile().isActive()).isTrue();
        verify(trainerRepository).save(created.profile());
        verify(metrics).recordTrainerRegistration();
    }

    @Test
    void createRejectsUnknownSpecializationBeforeSaving() {
        when(trainingTypeRepository.findByName(TrainingTypeName.CARDIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                "Bob", "Trainer", TrainingTypeName.CARDIO))
                .isInstanceOf(EntityNotFoundException.class);
        verify(trainerRepository, never()).save(any());
    }

    @Test
    void findByUsernameReturnsAuthenticatedTrainerOnlyForOwnUsername() {
        Trainer trainer = trainer("Bob.Trainer", TrainingTypeName.CARDIO, true);
        when(trainerRepository.findByUserUsernameIgnoreCase("bob.trainer")).thenReturn(Optional.of(trainer));

        assertThat(service.findByUsername("bob.trainer")).isSameAs(trainer);
    }

    @Test
    void updateMutatesTrainerButKeepsSpecializationReadOnly() {
        Trainer trainer = trainer("Bob.Trainer", TrainingTypeName.CARDIO, true);
        TrainingType originalSpecialization = trainer.getSpecialization();
        when(trainerRepository.findByUserUsernameIgnoreCase("Bob.Trainer")).thenReturn(Optional.of(trainer));

        service.update("Bob.Trainer", "Robert", "Trainer", false);

        assertThat(trainer.getFirstName()).isEqualTo("Robert");
        assertThat(trainer.getSpecialization()).isSameAs(originalSpecialization);
        assertThat(trainer.isActive()).isFalse();
        verify(trainingTypeRepository, never()).findByName(any());
    }

    @Test
    void getTrainingsAuthenticatesOwnProfileAndDelegatesCriteria() {
        Trainer trainer = trainer("Coach.One", TrainingTypeName.YOGA, true);
        Trainee trainee = new Trainee(new User("Runner", "One", "Runner.One", "secret1234", true),
                LocalDate.of(2000, 1, 1), "Address");
        Training training = new Training(trainee, trainer, "Yoga", trainer.getSpecialization(),
                LocalDate.of(2026, 7, 2), 45);
        when(trainingRepository.findTrainerTrainings(
                "Coach.One",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 3),
                "Runner")).thenReturn(List.of(training));

        assertThat(service.getTrainings(
                "Coach.One", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "Runner"))
                .containsExactly(training);
    }

    private Trainer trainer(String username, TrainingTypeName specialization, boolean active) {
        return new Trainer(new User("Bob", "Trainer", username, "secret1234", active),
                new TrainingType(specialization));
    }
}
