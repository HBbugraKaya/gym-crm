package com.example.gymcrm.web.controller;

import com.example.gymcrm.web.dto.*;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gymcrm.service.TrainerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerController {
    private final TrainerService trainerService;

    @PostMapping
    public RegistrationResponse register(@Valid @RequestBody TrainerRegistrationRequest request) {
        var created = trainerService.create(request.firstName(), request.lastName(), request.specialization());
        return new RegistrationResponse(created.profile().getUser().getUsername(), created.rawPassword());
    }

    @GetMapping("/{username}")
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        var trainer = trainerService.selectByUsername(username);
        var user = trainer.getUser();

        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainer.getSpecialization().getName());
    }

    @PutMapping("/{username}")
    public TrainerProfileResponse update(@PathVariable String username, @Valid @RequestBody TrainerUpdateRequest request) {
        var trainer = trainerService.update(username, request.firstName(), request.lastName(), request.active());
        var user = trainer.getUser();
        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainer.getSpecialization().getName());
    }

    @PatchMapping("/{username}")
    public void setActive(@PathVariable String username, @RequestBody ActiveStatusRequest request) {
        trainerService.setActive(username, request.active());
    }

    @GetMapping("/{username}/trainings")
    public List<TrainerTrainingResponse> getTrainings(
            @PathVariable String username,
            @RequestParam(required = false) LocalDate periodFrom,
            @RequestParam(required = false) LocalDate periodTo,
            @RequestParam(required = false) String traineeName) {
        var trainings = trainerService.getTrainings(username, periodFrom, periodTo, traineeName);
        return trainings.stream()
                .map(t -> new TrainerTrainingResponse(
                        t.getTrainingName(),
                        t.getTrainingType().getName(),
                        t.getTrainingDate(),
                        t.getTrainingDuration(),
                        t.getTrainee().getUser().getFirstName() + " "
                                + t.getTrainee().getUser().getLastName()))
                .toList();
    }

}
