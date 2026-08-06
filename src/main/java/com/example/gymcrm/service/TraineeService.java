package com.example.gymcrm.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.gymcrm.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gymcrm.entity.Trainee;
import com.example.gymcrm.entity.Trainer;
import com.example.gymcrm.entity.Training;
import com.example.gymcrm.entity.TrainingTypeName;
import com.example.gymcrm.entity.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.utility.PasswordGenerator;
import com.example.gymcrm.utility.UsernameGenerator;

@Service
@RequiredArgsConstructor
public class TraineeService {

    private final TraineeRepository traineeRepository;
    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final TrainingRepository trainingRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreatedAccount<Trainee> create(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        if (trainerRepository.existsByUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase(firstName, lastName)) {
            throw new IllegalArgumentException("Person is already registered as a trainer");
        }

        String username = usernameGenerator.generateUsername(firstName, lastName);
        String rawPassword = passwordGenerator.generatePassword();

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        User savedUser = userRepository.save(user);

        Trainee trainee = new Trainee();
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        trainee.setUser(savedUser);
        Trainee savedTrainee = traineeRepository.save(trainee);

        return new CreatedAccount<>(savedTrainee, rawPassword);
    }

    public Trainee selectByUsername(String username) {
        return traineeRepository
                .findByUserUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found"));
    }

    @Transactional
    public Trainee update(
            String username, String firstName, String lastName, LocalDate dateOfBirth, String address, boolean active) {
        Trainee trainee = selectByUsername(username);
        User user = trainee.getUser();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);

        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);

        return trainee;
    }

    @Transactional
    public Trainee updateTrainers(String traineeUsername, List<String> trainerUsernames) {
        Trainee trainee = selectByUsername(traineeUsername);

        Set<Trainer> trainers = new HashSet<>();
        for (String trainerUsername : trainerUsernames) {
            Trainer trainer = trainerRepository
                    .findByUserUsernameIgnoreCase(trainerUsername)
                    .orElseThrow(() -> new RuntimeException("Trainer not found"));
            trainers.add(trainer);
        }

        trainee.setTrainers(trainers);
        return trainee;
    }

    @Transactional
    public void deleteByUsername(String username) {
        Trainee trainee = selectByUsername(username);
        traineeRepository.delete(trainee);
    }

    @Transactional
    public List<Trainer> getUnassignedTrainers(String traineeUsername) {
        Trainee trainee = selectByUsername(traineeUsername);

        Set<Long> assignedIds =
                trainee.getTrainers().stream().map(Trainer::getId).collect(Collectors.toSet());

        if (assignedIds.isEmpty()) {
            return trainerRepository.findByUser_IsActiveTrue();
        }

        return trainerRepository.findByUser_IsActiveTrueAndIdNotIn(assignedIds);
    }

    public List<Training> getTrainings(String traineeUsername, LocalDate from, LocalDate to, String trainerName, TrainingTypeName trainingType) {

        return trainingRepository.findByTrainee_User_UsernameIgnoreCase(traineeUsername).stream()
                .filter(t -> from == null || !t.getTrainingDate().isBefore(from))
                .filter(t -> to == null || !t.getTrainingDate().isAfter(to))
                .filter(t -> trainerName == null
                        || t.getTrainer().getUser().getUsername().equalsIgnoreCase(trainerName))
                .filter(t -> trainingType == null || t.getTrainingType().getName() == trainingType)
                .toList();
    }

    @Transactional
    public void setActive(String traineeUsername, boolean active){
        Trainee trainee = selectByUsername(traineeUsername);
        trainee.getUser().setActive(active);
    }
}
