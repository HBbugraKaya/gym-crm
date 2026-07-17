package com.example.gymcrm.web.controller;

import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.web.dto.AddTrainingRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainings")
@Api(tags = "Trainings")
public class TrainingController {
    private final GymFacade gymFacade;

    public TrainingController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping
    @ApiOperation(value = "Add a training",
            notes = "The authenticated trainer must match the requested trainer. Training type is the trainer specialization.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Training added"),
            @ApiResponse(code = 400, message = "Request is invalid"),
            @ApiResponse(code = 401, message = "Trainer credentials are invalid"),
            @ApiResponse(code = 404, message = "Trainee or training type was not found")
    })
    public ResponseEntity<Void> addTraining(@Valid @RequestBody AddTrainingRequest request) {
        gymFacade.addTraining(new AddTrainingCommand(
                request.traineeUsername(),
                request.trainerUsername(),
                request.trainingName(),
                request.trainingDate(),
                request.durationMinutes()));
        return ResponseEntity.ok().build();
    }
}
