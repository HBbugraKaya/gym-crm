package com.example.gymcrm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.entity.Training;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    @EntityGraph(attributePaths = {"trainer.user", "trainingType"})
    List<Training> findByTrainee_User_UsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"trainee.user"})
    List<Training> findByTrainer_User_UsernameIgnoreCase(String username);
}
