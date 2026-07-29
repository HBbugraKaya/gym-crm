package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.web.OpenApiConfig;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.UpdateTrainerRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/trainers")
@Tag(name = "Trainers", description = "Trainer registration, profiles and training history")
@RequiredArgsConstructor
public class TrainerController {
    private final TrainerService trainerService;
    private final GymWebMapper mapper;

    @PostMapping
    @Operation(summary = "Register a trainer")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        var created = trainerService.create(
                request.firstName(), request.lastName(), request.specialization());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTrainerProfile(trainerService.findByUsername(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainer profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public TrainerProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request) {
        var updated = trainerService.update(username, request.firstName(), request.lastName(), request.active());
        return mapper.toTrainerProfile(updated);
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get a trainer's trainings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public List<TrainerTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) LocalDate periodFrom,
            @RequestParam(required = false) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {
        return mapper.toTrainerTrainings(trainerService.getTrainings(username, periodFrom, periodTo, traineeName));
    }
}
