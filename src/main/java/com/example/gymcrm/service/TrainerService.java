package com.example.gymcrm.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gymcrm.entity.Trainer;
import com.example.gymcrm.entity.Training;
import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.entity.TrainingTypeName;
import com.example.gymcrm.entity.User;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.utility.PasswordGenerator;
import com.example.gymcrm.utility.UsernameGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final TrainingRepository trainingRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreatedAccount<Trainer> create(String firstName, String lastName, TrainingTypeName specialization) {
        TrainingType trainingType = trainingTypeRepository
                .findByName(specialization)
                .orElseThrow(() -> new RuntimeException("Training type not found: " + specialization));

        String username = usernameGenerator.generateUsername(firstName, lastName);
        String rawPassword = passwordGenerator.generatePassword();

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        User savedUser = userRepository.save(user);

        Trainer trainer = new Trainer();
        trainer.setSpecialization(trainingType);
        trainer.setUser(savedUser);
        Trainer savedTrainer = trainerRepository.save(trainer);

        return new CreatedAccount<>(savedTrainer, rawPassword);
    }

    public Trainer selectByUsername(String username) {
        return trainerRepository
                .findByUserUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
    }

    @Transactional
    public Trainer update(String username, String firstName, String lastName, boolean active) {
        Trainer trainer = selectByUsername(username);
        User user = trainer.getUser();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);

        return trainer;
    }

    public List<Training> getTrainings(String trainerUsername, LocalDate from, LocalDate to, String traineeName) {
        return trainingRepository.findByTrainer_User_UsernameIgnoreCase(trainerUsername).stream()
                .filter(t -> from == null || !t.getTrainingDate().isBefore(from))
                .filter(t -> to == null || !t.getTrainingDate().isAfter(to))
                .filter(t -> traineeName == null
                        || t.getTrainee().getUser().getUsername().equalsIgnoreCase(traineeName))
                .toList();
    }

    @Transactional
    public void setActive(String trainerUsername, boolean active) {
        Trainer trainer = selectByUsername(trainerUsername);
        trainer.getUser().setActive(active);
    }
}
