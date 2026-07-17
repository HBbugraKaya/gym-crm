package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRepository extends JpaRepository<Training, Long>, TrainingRepositoryCustom {
}
