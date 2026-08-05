package com.example.gymcrm.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.service.TrainingTypeService;
import com.example.gymcrm.web.dto.TrainingTypeResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training-types")
public class TrainingTypeController {
    private final TrainingTypeService trainingTypeService;

    @GetMapping
    public List<TrainingTypeResponse> getTrainingTypes() {
        List<TrainingType> types = trainingTypeService.getTrainingTypes();

        return types.stream()
                .map(t -> new TrainingTypeResponse(t.getId(), t.getName()))
                .toList();
    }
}
