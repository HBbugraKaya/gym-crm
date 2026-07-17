package com.example.gymcrm.web.mapper;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.service.CreatedAccount;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TraineeProfileResponse;
import com.example.gymcrm.web.dto.TraineeSummaryResponse;
import com.example.gymcrm.web.dto.TraineeTrainingResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerSummaryResponse;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GymWebMapper {

    public RegistrationResponse toRegistrationResponse(CreatedAccount<?> created) {
        String username = switch (created.profile()) {
            case Trainee trainee -> trainee.getUsername();
            case Trainer trainer -> trainer.getUsername();
            default -> throw new IllegalArgumentException("Unsupported profile type");
        };
        return new RegistrationResponse(username, created.rawPassword());
    }

    public TraineeProfileResponse toTraineeProfile(Trainee trainee) {
        return new TraineeProfileResponse(
                trainee.getUsername(),
                trainee.getFirstName(),
                trainee.getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.isActive(),
                trainee.getTrainers().stream().map(this::toTrainerSummary).toList());
    }

    public TrainerProfileResponse toTrainerProfile(Trainer trainer) {
        return new TrainerProfileResponse(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getSpecialization().getName(),
                trainer.isActive(),
                trainer.getTrainees().stream().map(this::toTraineeSummary).toList());
    }

    public TrainerSummaryResponse toTrainerSummary(Trainer trainer) {
        return new TrainerSummaryResponse(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getSpecialization().getName());
    }

    public List<TrainerSummaryResponse> toTrainerSummaries(List<Trainer> trainers) {
        return trainers.stream().map(this::toTrainerSummary).toList();
    }

    public TraineeSummaryResponse toTraineeSummary(Trainee trainee) {
        return new TraineeSummaryResponse(
                trainee.getUsername(),
                trainee.getFirstName(),
                trainee.getLastName());
    }

    public List<TraineeTrainingResponse> toTraineeTrainings(List<Training> trainings) {
        return trainings.stream().map(this::toTraineeTraining).toList();
    }

    public List<TrainerTrainingResponse> toTrainerTrainings(List<Training> trainings) {
        return trainings.stream().map(this::toTrainerTraining).toList();
    }

    public List<TrainingTypeResponse> toTrainingTypes(List<TrainingType> trainingTypes) {
        return trainingTypes.stream()
                .map(type -> new TrainingTypeResponse(type.getId(), type.getName()))
                .toList();
    }

    private TraineeTrainingResponse toTraineeTraining(Training training) {
        return new TraineeTrainingResponse(
                training.getName(),
                training.getDate(),
                training.getTrainingType().getName(),
                training.getDurationMinutes(),
                fullName(training.getTrainer().getFirstName(), training.getTrainer().getLastName()));
    }

    private TrainerTrainingResponse toTrainerTraining(Training training) {
        return new TrainerTrainingResponse(
                training.getName(),
                training.getDate(),
                training.getTrainingType().getName(),
                training.getDurationMinutes(),
                fullName(training.getTrainee().getFirstName(), training.getTrainee().getLastName()));
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
