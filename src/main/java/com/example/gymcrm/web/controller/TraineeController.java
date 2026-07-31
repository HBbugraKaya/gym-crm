package com.example.gymcrm.web.controller;

import com.example.gymcrm.web.dto.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gymcrm.service.TraineeService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trainees")

public class TraineeController {
    private final TraineeService traineeService;

    @PostMapping
    public RegistrationResponse register(@RequestBody TraineeRegistrationRequest request) {
        var created = traineeService.create(request.firstName(), request.lastName(), request.dateOfBirth(),
                request.address());
        return new RegistrationResponse(created.profile().getUser().getUsername(), created.rawPassword());
    }

    @GetMapping("/{username}")
    public TraineeProfileResponse getProfile(@PathVariable String username) {
        var trainee = traineeService.selectByUsername(username);
        var user = trainee.getUser();

        return new TraineeProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainee.getDateOfBirth(), trainee.getAddress());
    }

    @PutMapping("/{username}")
    public TraineeProfileResponse update(@PathVariable String username, @RequestBody TraineeUpdateRequest request) {
        var trainee = traineeService.update(username, request.firstName(), request.lastName(), request.dateOfBirth(),
                request.address(), request.active());
        var user = trainee.getUser();
        return new TraineeProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainee.getDateOfBirth(), trainee.getAddress());
    }

    @DeleteMapping("/{username}")
    public void delete(@PathVariable String username) {
        traineeService.deleteByUsername(username);
    }

    @GetMapping("/{username}/available-trainers")
    public List<TrainerSummaryResponse> getAvailableTrainers(@PathVariable String username) {
        var trainers = traineeService.getUnassignedTrainers(username);
        return trainers.stream()
                .map(t -> {
                    var user = t.getUser();
                    return new TrainerSummaryResponse(
                            user.getUsername(),
                            user.getFirstName(),
                            user.getLastName(),
                            t.getSpecialization().getName()
                    );
                })
                .toList();
    }

    @PutMapping("/{username}/trainers")
    public List<TrainerSummaryResponse> updateTrainers(
            @PathVariable String username,
            @RequestBody TrainerAssignmentsRequest request) {

        var trainee = traineeService.updateTrainers(username, request.trainerUsernames());

        return trainee.getTrainers().stream()
                .map(t -> {
                    var user = t.getUser();
                    return new TrainerSummaryResponse(
                            user.getUsername(),
                            user.getFirstName(),
                            user.getLastName(),
                            t.getSpecialization().getName()
                    );
                })
                .toList();
    }
}
