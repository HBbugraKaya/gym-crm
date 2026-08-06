package com.example.gymcrm.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.entity.Trainer;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
    Optional<Trainer> findByUserUsernameIgnoreCase(String username);

    boolean existsByUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase(String firstName, String lastName);

    List<Trainer> findByUser_IsActiveTrue();

    List<Trainer> findByUser_IsActiveTrueAndIdNotIn(Collection<Long> ids);
}
