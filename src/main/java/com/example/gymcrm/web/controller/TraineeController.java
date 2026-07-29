package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.TraineeService;
import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.OpenApiConfig;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.ChangeStatusRequest;
import com.example.gymcrm.web.dto.TraineeProfileResponse;
import com.example.gymcrm.web.dto.TraineeRegistrationRequest;
import com.example.gymcrm.web.dto.TraineeTrainingResponse;
import com.example.gymcrm.web.dto.TrainerAssignmentsRequest;
import com.example.gymcrm.web.dto.TrainerSummaryResponse;
import com.example.gymcrm.web.dto.UpdateTraineeRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequiredArgsConstructor
public class TraineeController {
    private final TraineeService traineeService;
    private final UserAccountService userAccountService;
    private final GymWebMapper mapper;

    @PostMapping
    @Operation(summary = "Register a trainee")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TraineeRegistrationRequest request) {
        var created = traineeService.create(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainee profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public TraineeProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTraineeProfile(traineeService.findByUsername(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainee profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public TraineeProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTraineeRequest request) {
        var trainee = traineeService.update(
                username,
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.address(),
                request.active());
        return mapper.toTraineeProfile(trainee);
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete a trainee profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public ResponseEntity<Void> deleteProfile(@PathVariable String username) {
        traineeService.deleteByUsername(username);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or deactivate a trainee")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public ResponseEntity<Void> changeStatus(
            @PathVariable String username,
            @Valid @RequestBody ChangeStatusRequest request) {
        userAccountService.changeStatus(username, request.active());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/available-trainers")
    @Operation(summary = "Get active trainers not assigned to a trainee")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public List<TrainerSummaryResponse> getAvailableTrainers(@PathVariable String username) {
        return mapper.toTrainerSummaries(traineeService.getUnassignedTrainers(username));
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Replace a trainee's trainer assignments")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public List<TrainerSummaryResponse> updateTrainers(
            @PathVariable String username,
            @Valid @RequestBody TrainerAssignmentsRequest request) {
        return mapper.toTrainerSummaries(
                traineeService.updateTrainers(username, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get a trainee's trainings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public List<TraineeTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) LocalDate periodFrom,
            @RequestParam(required = false) LocalDate periodTo,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) TrainingTypeName trainingType) {
        return mapper.toTraineeTrainings(
                traineeService.getTrainings(username, periodFrom, periodTo, trainerName, trainingType));
    }
}
