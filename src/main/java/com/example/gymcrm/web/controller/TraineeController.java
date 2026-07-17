package com.example.gymcrm.web.controller;

import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TraineeProfileResponse;
import com.example.gymcrm.web.dto.TraineeRegistrationRequest;
import com.example.gymcrm.web.dto.TraineeTrainingResponse;
import com.example.gymcrm.web.dto.TrainerAssignmentsRequest;
import com.example.gymcrm.web.dto.TrainerSummaryResponse;
import com.example.gymcrm.web.dto.UpdateTraineeRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import com.example.gymcrm.domain.TrainingTypeName;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trainees")
@Api(tags = "Trainees")
public class TraineeController {
    private final GymFacade gymFacade;
    private final GymWebMapper mapper;

    public TraineeController(GymFacade gymFacade, GymWebMapper mapper) {
        this.gymFacade = gymFacade;
        this.mapper = mapper;
    }

    @PostMapping
    @ApiOperation(value = "Register a trainee",
            notes = "Public endpoint that generates and returns a username and password.",
            response = RegistrationResponse.class)
    @ApiResponses({
            @ApiResponse(code = 201, message = "Trainee registered"),
            @ApiResponse(code = 400, message = "Request validation failed")
    })
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TraineeRegistrationRequest request) {
        var created = gymFacade.createTrainee(new CreateTraineeCommand(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address(), true));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @ApiOperation(value = "Get trainee profile", response = TraineeProfileResponse.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainee profile returned"),
            @ApiResponse(code = 400, message = "Authenticated trainee differs from the requested username"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid")
    })
    public TraineeProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTraineeProfile(gymFacade.getTraineeProfile(username));
    }

    @PutMapping("/{username}")
    @ApiOperation(value = "Update trainee profile",
            notes = "Replaces editable profile fields. Username cannot be changed.",
            response = TraineeProfileResponse.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainee profile updated"),
            @ApiResponse(code = 400, message = "Request validation failed or usernames differ"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid")
    })
    public TraineeProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTraineeRequest request) {
        var trainee = gymFacade.updateTrainee(username, new UpdateTraineeCommand(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address(), request.active()));
        return mapper.toTraineeProfile(trainee);
    }

    @DeleteMapping("/{username}")
    @ApiOperation(value = "Delete trainee profile",
            notes = "Hard-deletes the trainee, its user record, assignments, and relevant trainings.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainee deleted"),
            @ApiResponse(code = 400, message = "Authenticated trainee differs from the requested username"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid")
    })
    public ResponseEntity<Void> deleteProfile(@PathVariable String username) {
        gymFacade.deleteTrainee(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/available-trainers")
    @ApiOperation(value = "Get active trainers not assigned to the trainee",
            response = TrainerSummaryResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Available trainers returned"),
            @ApiResponse(code = 400, message = "Authenticated trainee differs from the requested username"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid"),
            @ApiResponse(code = 404, message = "Trainee was not found")
    })
    public List<TrainerSummaryResponse> getAvailableTrainers(@PathVariable String username) {
        return mapper.toTrainerSummaries(gymFacade.getUnassignedTrainers(username));
    }

    @PutMapping("/{username}/trainers")
    @ApiOperation(value = "Update trainee trainer list",
            notes = "Replaces the complete trainer assignment list.",
            response = TrainerSummaryResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainer assignments updated"),
            @ApiResponse(code = 400, message = "Request validation failed or usernames differ"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid"),
            @ApiResponse(code = 404, message = "Trainee or a requested trainer was not found")
    })
    public List<TrainerSummaryResponse> updateTrainers(
            @PathVariable String username,
            @Valid @RequestBody TrainerAssignmentsRequest request) {
        return mapper.toTrainerSummaries(
                gymFacade.updateTraineeTrainers(username, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    @ApiOperation(value = "Get trainee trainings",
            notes = "Supports optional date, trainer-name, and training-type filters.",
            response = TraineeTrainingResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainings returned"),
            @ApiResponse(code = 400, message = "Filter values are invalid or usernames differ"),
            @ApiResponse(code = 401, message = "Trainee credentials are invalid")
    })
    public List<TraineeTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingTypeName trainingType) {
        var criteria = new TraineeTrainingCriteria(periodFrom, periodTo, trainerName, trainingType);
        return mapper.toTraineeTrainings(gymFacade.getTraineeTrainings(username, criteria));
    }
}
