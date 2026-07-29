package com.example.gymcrm.service;

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
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerService {
    private final TrainerRepository trainerRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final TrainingRepository trainingRepository;
    private final UniqueUsernameGenerator usernameGenerator;
    private final SecurePasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final GymCrmMetrics metrics;

    @Transactional
    public CreatedAccount<Trainer> create(
            String firstName, String lastName, TrainingTypeName specializationName) {
        TrainingType specialization = findTrainingType(specializationName);
        String username = usernameGenerator.generate(firstName, lastName);
        String rawPassword = passwordGenerator.generate();

        Trainer trainer = new Trainer(
                new User(firstName, lastName, username, passwordEncoder.encode(rawPassword), true),
                specialization);
        Trainer saved = trainerRepository.save(trainer);
        metrics.recordTrainerRegistration();
        return new CreatedAccount<>(saved, rawPassword);
    }

    @PreAuthorize("hasRole('TRAINER') and #username.equalsIgnoreCase(authentication.name)")
    public Trainer findByUsername(String username) {
        return find(username);
    }

    @Transactional
    @PreAuthorize("hasRole('TRAINER') and #username.equalsIgnoreCase(authentication.name)")
    public Trainer update(String username, String firstName, String lastName, boolean active) {
        Trainer trainer = find(username);
        trainer.updateProfile(firstName, lastName, active);
        return trainer;
    }

    @PreAuthorize("hasRole('TRAINER') and #trainerUsername.equalsIgnoreCase(authentication.name)")
    public List<Training> getTrainings(
            String trainerUsername, LocalDate fromDate, LocalDate toDate, String traineeName) {
        validatePeriod(fromDate, toDate);
        return trainingRepository.findTrainerTrainings(
                trainerUsername, fromDate, toDate, normalizeName(traineeName));
    }

    private TrainingType findTrainingType(TrainingTypeName name) {
        return trainingTypeRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("TrainingType", name.name()));
    }

    private Trainer find(String username) {
        return trainerRepository.findByUserUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer", username));
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
