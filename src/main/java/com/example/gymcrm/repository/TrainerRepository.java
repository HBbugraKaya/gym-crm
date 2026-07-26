package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    @EntityGraph(attributePaths = {"user", "specialization", "trainees.user"})
    Optional<Trainer> findByUserUsernameIgnoreCase(String username);

    boolean existsByUserUsernameIgnoreCase(String username);

    @Query("SELECT t FROM Trainer t WHERE t.user.active = true AND t NOT IN (SELECT tr FROM Trainee trainee JOIN trainee.trainers tr WHERE lower(trainee.user.username) = lower(:traineeUsername))")
    List<Trainer> findUnassignedActiveTrainers(String traineeUsername);
}
