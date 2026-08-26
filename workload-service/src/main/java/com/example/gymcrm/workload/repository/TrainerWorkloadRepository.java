package com.example.gymcrm.workload.repository;

import com.example.gymcrm.workload.domain.TrainerWorkload;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TrainerWorkloadRepository extends MongoRepository<TrainerWorkload, String> {
    Optional<TrainerWorkload> findByTrainerUsernameIgnoreCase(String trainerUsername);
}
