package com.example.gymcrm.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gymcrm.client.TrainerWorkloadClient;
import com.example.gymcrm.entity.Trainee;
import com.example.gymcrm.entity.Trainer;
import com.example.gymcrm.entity.Training;
import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.entity.TrainingTypeName;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.TrainingRepository;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.web.dto.ActionType;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainingService {

        private final TrainingRepository trainingRepository;
        private final TraineeRepository traineeRepository;
        private final TrainerRepository trainerRepository;
        private final TrainingTypeRepository trainingTypeRepository;
        private final TrainerWorkloadClient trainerWorkloadClient;

        @Transactional
        public Training create(String traineeUsername, String trainerUsername, String trainingName,
                        TrainingTypeName trainingType, LocalDate trainingDate, int duration) {

                Trainee trainee = traineeRepository
                                .findByUserUsernameIgnoreCase(traineeUsername)
                                .orElseThrow(() -> new EntityNotFoundException("Trainee not found"));
                Trainer trainer = trainerRepository
                                .findByUserUsernameIgnoreCase(trainerUsername)
                                .orElseThrow(() -> new EntityNotFoundException("Trainer not found"));
                TrainingType type = trainingTypeRepository
                                .findByName(trainingType)
                                .orElseThrow(() -> new EntityNotFoundException("Training not found"));

                Training training = new Training();
                training.setTrainee(trainee);
                training.setTrainer(trainer);
                training.setTrainingName(trainingName);
                training.setTrainingType(type);
                training.setTrainingDate(trainingDate);
                training.setTrainingDuration(duration);

                trainerWorkloadClient.updateWorkload(new TrainerWorkloadRequest(
                                trainer.getUser().getUsername(),
                                trainer.getUser().getFirstName(),
                                trainer.getUser().getLastName(),
                                trainer.getUser().isActive(),
                                trainingDate,
                                duration,
                                ActionType.ADD));

                return trainingRepository.save(training);
        }
}
