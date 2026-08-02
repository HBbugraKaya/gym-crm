package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.web.dto.AddTrainingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor

public class TrainingController {
    private final TrainingService trainingService;

    @PostMapping
    void addTraining(@RequestBody AddTrainingRequest request) {
        trainingService.create(
                request.traineeUsername(),
                request.trainerUsername(),
                request.trainingName(),
                request.trainingType(),
                request.trainingDate(),
                request.duration()
        );
    }
}
