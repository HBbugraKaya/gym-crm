package com.example.gymcrm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;

public interface TrainingTypeRepository extends JpaRepository<TrainingType, Long> {
    Optional<TrainingType> findByName(TrainingTypeName name);

    boolean existsByName(TrainingTypeName name);
}
