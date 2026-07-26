package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @EntityGraph(attributePaths = {"user", "trainers.user", "trainers.specialization"})
    Optional<Trainee> findByUserUsernameIgnoreCase(String username);

    boolean existsByUserUsernameIgnoreCase(String username);
}
