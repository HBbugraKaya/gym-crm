package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    @EntityGraph(attributePaths = {"trainer.user", "trainingType"})
    List<Training> findByTraineeUserUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"trainee.user", "trainingType"})
    List<Training> findByTrainerUserUsernameIgnoreCase(String username);
}
