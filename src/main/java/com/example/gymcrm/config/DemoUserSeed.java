package com.example.gymcrm.config;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.gymcrm.entity.Trainee;
import com.example.gymcrm.entity.Trainer;
import com.example.gymcrm.entity.TrainingTypeName;
import com.example.gymcrm.entity.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(2)
@RequiredArgsConstructor
public class DemoUserSeed implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedTrainee("demo.trainee", "Demo", "Trainee");
        seedTrainer("demo.trainer", "Demo", "Trainer");
    }

    private void seedTrainee(String username, String firstName, String lastName) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setActive(true);
        User saved = userRepository.save(user);

        Trainee trainee = new Trainee();
        trainee.setDateOfBirth(LocalDate.of(1995, 1, 15));
        trainee.setAddress("Demo Street 1");
        trainee.setUser(saved);
        traineeRepository.save(trainee);
    }

    private void seedTrainer(String username, String firstName, String lastName) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setActive(true);
        User saved = userRepository.save(user);

        Trainer trainer = new Trainer();
        trainer.setSpecialization(trainingTypeRepository
                .findByName(TrainingTypeName.FITNESS)
                .orElseThrow(() -> new IllegalStateException("FITNESS training type missing")));
        trainer.setUser(saved);
        trainerRepository.save(trainer);
    }
}
