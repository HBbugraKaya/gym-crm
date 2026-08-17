package com.example.gymcrm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.entity.Trainee;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @EntityGraph(attributePaths = {"trainers"})
    Optional<Trainee> findByUserUsernameIgnoreCase(String username);

    boolean existsByUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase(String firstName, String lastName);
}
