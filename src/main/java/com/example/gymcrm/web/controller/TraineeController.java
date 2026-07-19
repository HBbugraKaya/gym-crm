package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import com.example.gymcrm.service.TraineeService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Trainees", description = "Trainee registration, profiles, assignments and training history")
public class TraineeController {
    private final TraineeService traineeService;
    private final GymWebMapper mapper;

    public TraineeController(TraineeService traineeService, GymWebMapper mapper) {
        this.traineeService = traineeService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Register a trainee",
            description = "Public endpoint that generates and returns a username and password.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee registered"),
            @ApiResponse(responseCode = "400", description = "Request validation failed")
    })
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TraineeRegistrationRequest request) {
        var created = traineeService.create(new CreateTraineeCommand(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address(), true));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainee profile")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile returned"),
            @ApiResponse(responseCode = "400", description = "Authenticated trainee differs from the requested username"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid")
    })
    public TraineeProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTraineeProfile(traineeService.findByUsername(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainee profile",
            description = "Replaces editable profile fields. Username cannot be changed.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed or usernames differ"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid")
    })
    public TraineeProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTraineeRequest request) {
        var trainee = traineeService.update(username, new UpdateTraineeCommand(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address(), request.active()));
        return mapper.toTraineeProfile(trainee);
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete trainee profile",
            description = "Hard-deletes the trainee, its user record, assignments, and relevant trainings.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee deleted"),
            @ApiResponse(responseCode = "400", description = "Authenticated trainee differs from the requested username"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid")
    })
    public ResponseEntity<Void> deleteProfile(@PathVariable String username) {
        traineeService.deleteByUsername(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/available-trainers")
    @Operation(summary = "Get active trainers not assigned to the trainee")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available trainers returned"),
            @ApiResponse(responseCode = "400", description = "Authenticated trainee differs from the requested username"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid"),
            @ApiResponse(responseCode = "404", description = "Trainee was not found")
    })
    public List<TrainerSummaryResponse> getAvailableTrainers(@PathVariable String username) {
        return mapper.toTrainerSummaries(traineeService.getUnassignedTrainers(username));
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Update trainee trainer list",
            description = "Replaces the complete trainer assignment list.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer assignments updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed or usernames differ"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid"),
            @ApiResponse(responseCode = "404", description = "Trainee or a requested trainer was not found")
    })
    public List<TrainerSummaryResponse> updateTrainers(
            @PathVariable String username,
            @Valid @RequestBody TrainerAssignmentsRequest request) {
        return mapper.toTrainerSummaries(
                traineeService.updateTrainers(username, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainee trainings",
            description = "Supports optional date, trainer-name, and training-type filters.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "400", description = "Filter values are invalid or usernames differ"),
            @ApiResponse(responseCode = "401", description = "Trainee credentials are invalid")
    })
    public List<TraineeTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingTypeName trainingType) {
        var criteria = new TraineeTrainingCriteria(periodFrom, periodTo, trainerName, trainingType);
        return mapper.toTraineeTrainings(traineeService.getTrainings(username, criteria));
    }
}
