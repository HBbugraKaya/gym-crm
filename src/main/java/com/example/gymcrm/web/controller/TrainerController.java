package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.UpdateTrainerRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
public class TrainerController {
    private final TrainerService trainerService;
    private final GymWebMapper mapper;

    public TrainerController(TrainerService trainerService, GymWebMapper mapper) {
        this.trainerService = trainerService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Register a trainer",
            description = "Public endpoint that generates and returns a username and password.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer registered"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "404", description = "Specialization was not found")
    })
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        var created = trainerService.create(
                request.firstName(), request.lastName(), request.specialization());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get trainer profile")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile returned"),
            @ApiResponse(responseCode = "400", description = "Authenticated trainer differs from the requested username"),
            @ApiResponse(responseCode = "401", description = "Trainer credentials are invalid")
    })
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTrainerProfile(trainerService.findByUsername(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update trainer profile",
            description = "Replaces editable profile fields. Username and specialization are read-only.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed or usernames differ"),
            @ApiResponse(responseCode = "401", description = "Trainer credentials are invalid")
    })
    public TrainerProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request) {
        var updated = trainerService.update(
                username, request.firstName(), request.lastName(), request.active());
        return mapper.toTrainerProfile(updated);
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "Get trainer trainings",
            description = "Supports optional date and trainee-name filters.")
    @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "400", description = "Filter values are invalid or usernames differ"),
            @ApiResponse(responseCode = "401", description = "Trainer credentials are invalid")
    })
    public List<TrainerTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {
        var criteria = new TrainerTrainingCriteria(periodFrom, periodTo, traineeName);
        return mapper.toTrainerTrainings(trainerService.getTrainings(username, criteria));
    }
}
