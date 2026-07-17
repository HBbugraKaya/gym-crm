package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;

import java.util.List;

public interface TrainingRepositoryCustom {

    List<Training> findByTraineeUsername(String traineeUsername, TraineeTrainingCriteria criteria);

    List<Training> findByTrainerUsername(String trainerUsername, TrainerTrainingCriteria criteria);
}
