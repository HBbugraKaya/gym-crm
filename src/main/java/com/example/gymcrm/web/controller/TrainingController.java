package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.web.dto.AddTrainingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainings")
@Tag(name = "Trainings", description = "Training session management")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class TrainingController {
    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "The authenticated trainer must match the requested trainer. Training type is the trainer specialization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added"),
            @ApiResponse(responseCode = "400", description = "Request is invalid"),
            @ApiResponse(responseCode = "401", description = "Trainer credentials are invalid"),
            @ApiResponse(responseCode = "404", description = "Trainee or training type was not found")
    })
    public ResponseEntity<Void> addTraining(@Valid @RequestBody AddTrainingRequest request) {
        trainingService.addTraining(
                request.traineeUsername(),
                request.trainerUsername(),
                request.trainingName(),
                request.trainingDate(),
                request.durationMinutes());
        return ResponseEntity.ok().build();
    }
}
