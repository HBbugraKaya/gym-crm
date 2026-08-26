package com.example.gymcrm.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gymcrm.service.TrainerWorkloadService;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/workload")
@RequiredArgsConstructor
public class TrainerWorkloadController {

    private final TrainerWorkloadService trainerWorkloadService;

    @PostMapping
    public void addTrainerWorkload(@Valid @RequestBody TrainerWorkloadRequest request) {
        trainerWorkloadService.processWorkload(request);
    }

    @GetMapping("/{username}/{year}/{month}")
    public int getTrainerWorkload(@PathVariable String username, @PathVariable int year, @PathVariable int month) {
        return trainerWorkloadService.getMonthlyDuration(username, year, month);
    }
    
    
}
