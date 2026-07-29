package com.example.gymcrm.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerUpdateRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
public class TrainerController {
    private final TrainerService trainerService;

    @PostMapping
    public RegistrationResponse register(@RequestBody TrainerRegistrationRequest request) {
        var created = trainerService.create(request.firstName(), request.lastName(), request.specialization());
        return new RegistrationResponse(created.profile().getUser().getUsername(), created.rawPassword());
    }

    @GetMapping("/{username}")
    public TrainerProfileResponse getProfile(@PathVariable String username) {
        var trainer = trainerService.selectByUsername(username);
        var user = trainer.getUser();

        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(), trainer.getSpecialization().getName());
    }

    @PutMapping("/{username}")
    public TrainerProfileResponse update(@PathVariable String username, @RequestBody TrainerUpdateRequest request) {
        var trainer = trainerService.update(username, request.firstName(), request.lastName(), request.active());
        var user = trainer.getUser();
        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(), trainer.getSpecialization().getName());
    }
}
