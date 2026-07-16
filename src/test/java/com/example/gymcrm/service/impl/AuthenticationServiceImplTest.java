package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.service.command.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @InjectMocks
    private AuthenticationServiceImpl service;

    @Test
    void traineeCredentialsMatchChecksPassword() {
        Trainee trainee = trainee("John.Smith", "secret1234");
        when(traineeRepository.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        assertThat(service.traineeCredentialsMatch(new Credentials("John.Smith", "secret1234"))).isTrue();
        assertThat(service.traineeCredentialsMatch(new Credentials("John.Smith", "wrong"))).isFalse();

        verify(traineeRepository, times(2)).findByUsername("John.Smith");
    }

    @Test
    void trainerCredentialsMatchReturnsFalseWhenTrainerDoesNotExist() {
        when(trainerRepository.findByUsername("Missing.Trainer")).thenReturn(Optional.empty());

        assertThat(service.trainerCredentialsMatch(new Credentials("Missing.Trainer", "secret1234"))).isFalse();
    }

    @Test
    void authenticateTraineeReturnsProfileForValidCredentials() {
        Trainee trainee = trainee("Jane.Doe", "secret1234");
        when(traineeRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(trainee));

        assertThat(service.authenticateTrainee(new Credentials("Jane.Doe", "secret1234"))).isSameAs(trainee);
    }

    @Test
    void authenticateTrainerRejectsInvalidPassword() {
        Trainer trainer = trainer("Bob.Trainer", "secret1234");
        when(trainerRepository.findByUsername("Bob.Trainer")).thenReturn(Optional.of(trainer));

        assertThatThrownBy(() -> service.authenticateTrainer(new Credentials("Bob.Trainer", "bad-password")))
                .isInstanceOf(AuthenticationException.class);
    }

    private Trainee trainee(String username, String password) {
        return new Trainee(new User("John", "Smith", username, password, true),
                LocalDate.of(2000, 1, 1), "Address");
    }

    private Trainer trainer(String username, String password) {
        return new Trainer(new User("Bob", "Trainer", username, password, true),
                new TrainingType(TrainingTypeName.CARDIO));
    }
}
