package com.example.gymcrm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.entity.TrainingTypeName;

public interface TrainingTypeRepository extends JpaRepository<TrainingType, Long> {
    Optional<TrainingType> findByName(TrainingTypeName name);

    boolean existsByName(TrainingTypeName name);
}
