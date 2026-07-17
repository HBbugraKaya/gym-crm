package com.example.gymcrm.web.controller;

import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.UpdateTrainerRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@Api(tags = "Trainers")
public class TrainerController {
    private final GymFacade gymFacade;
    private final GymWebMapper mapper;

    public TrainerController(GymFacade gymFacade, GymWebMapper mapper) {
        this.gymFacade = gymFacade;
        this.mapper = mapper;
    }

    @PostMapping
    @ApiOperation(value = "Register a trainer",
            notes = "Public endpoint that generates and returns a username and password.",
            response = RegistrationResponse.class)
    @ApiResponses({
            @ApiResponse(code = 201, message = "Trainer registered"),
            @ApiResponse(code = 400, message = "Request validation failed"),
            @ApiResponse(code = 404, message = "Specialization was not found")
    })
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        var created = gymFacade.createTrainer(new CreateTrainerCommand(
                request.firstName(), request.lastName(), request.specialization(), true));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegistrationResponse(created));
    }

    @GetMapping("/{username}")
    @ApiOperation(value = "Get trainer profile", response = TrainerProfileResponse.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainer profile returned"),
            @ApiResponse(code = 400, message = "Authenticated trainer differs from the requested username"),
            @ApiResponse(code = 401, message = "Trainer credentials are invalid")
    })
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        return mapper.toTrainerProfile(gymFacade.getTrainerProfile(username));
    }

    @PutMapping("/{username}")
    @ApiOperation(value = "Update trainer profile",
            notes = "Replaces editable profile fields. Username and specialization are read-only.",
            response = TrainerProfileResponse.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainer profile updated"),
            @ApiResponse(code = 400, message = "Request validation failed or usernames differ"),
            @ApiResponse(code = 401, message = "Trainer credentials are invalid")
    })
    public TrainerProfileResponse updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateTrainerRequest request) {
        var updated = gymFacade.updateTrainer(username, new UpdateTrainerCommand(
                request.firstName(),
                request.lastName(),
                request.active()));
        return mapper.toTrainerProfile(updated);
    }

    @GetMapping("/{username}/trainings")
    @ApiOperation(value = "Get trainer trainings",
            notes = "Supports optional date and trainee-name filters.",
            response = TrainerTrainingResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Trainings returned"),
            @ApiResponse(code = 400, message = "Filter values are invalid or usernames differ"),
            @ApiResponse(code = 401, message = "Trainer credentials are invalid")
    })
    public List<TrainerTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {
        var criteria = new TrainerTrainingCriteria(periodFrom, periodTo, traineeName);
        return mapper.toTrainerTrainings(gymFacade.getTrainerTrainings(username, criteria));
    }
}
