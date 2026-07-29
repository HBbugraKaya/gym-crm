package com.example.gymcrm.service;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TraineeService {
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final TrainingRepository trainingRepository;
    private final UniqueUsernameGenerator usernameGenerator;
    private final SecurePasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final GymCrmMetrics metrics;

    @Transactional
    public CreatedAccount<Trainee> create(
            String firstName, String lastName, LocalDate dateOfBirth, String address) {
        String username = usernameGenerator.generate(firstName, lastName);
        String rawPassword = passwordGenerator.generate();

        Trainee trainee = new Trainee(
                new User(firstName, lastName, username, passwordEncoder.encode(rawPassword), true),
                dateOfBirth,
                address);
        Trainee saved = traineeRepository.save(trainee);
        metrics.recordTraineeRegistration();
        return new CreatedAccount<>(saved, rawPassword);
    }

    @PreAuthorize("hasRole('TRAINEE') and #username.equalsIgnoreCase(authentication.name)")
    public Trainee findByUsername(String username) {
        return find(username);
    }

    @Transactional
    @PreAuthorize("hasRole('TRAINEE') and #username.equalsIgnoreCase(authentication.name)")
    public Trainee update(
            String username, String firstName, String lastName, LocalDate dateOfBirth, String address, boolean active) {
        Trainee trainee = find(username);
        trainee.updateProfile(firstName, lastName, dateOfBirth, address, active);
        return trainee;
    }

    @Transactional
    @PreAuthorize("hasRole('TRAINEE') and #username.equalsIgnoreCase(authentication.name)")
    public void deleteByUsername(String username) {
        Trainee trainee = find(username);
        traineeRepository.delete(trainee);
    }

    @PreAuthorize("hasRole('TRAINEE') and #traineeUsername.equalsIgnoreCase(authentication.name)")
    public List<Training> getTrainings(
            String traineeUsername,
            LocalDate fromDate,
            LocalDate toDate,
            String trainerName,
            TrainingTypeName trainingType) {
        validatePeriod(fromDate, toDate);
        return trainingRepository.findTraineeTrainings(
                traineeUsername, fromDate, toDate, normalizeName(trainerName), trainingType);
    }

    @PreAuthorize("hasRole('TRAINEE') and #traineeUsername.equalsIgnoreCase(authentication.name)")
    public List<Trainer> getUnassignedTrainers(String traineeUsername) {
        return trainerRepository.findUnassignedActiveTrainers(traineeUsername);
    }

    @Transactional
    @PreAuthorize("hasRole('TRAINEE') and #traineeUsername.equalsIgnoreCase(authentication.name)")
    public List<Trainer> updateTrainers(String traineeUsername, Collection<String> trainerUsernames) {
        Trainee trainee = find(traineeUsername);
        List<Trainer> trainers = trainerUsernames.stream()
                .map(username -> trainerRepository.findByUserUsernameIgnoreCase(username)
                        .orElseThrow(() -> new EntityNotFoundException("Trainer", username)))
                .distinct()
                .toList();
        trainee.replaceTrainers(trainers);
        return trainers;
    }

    private Trainee find(String username) {
        return traineeRepository.findByUserUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee", username));
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("periodFrom must be on or before periodTo");
        }
    }

    private String normalizeName(String name) {
        return name == null || name.isBlank() ? null : name.trim();
    }
}
