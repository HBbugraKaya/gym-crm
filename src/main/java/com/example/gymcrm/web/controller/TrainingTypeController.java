package com.example.gymcrm.web.controller;

import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.web.OpenApiConfig;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Tag(name = "Training types", description = "Immutable training-type catalog")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
@RequiredArgsConstructor
public class TrainingTypeController {
    private final TrainingTypeRepository trainingTypeRepository;
    private final GymWebMapper mapper;

    @GetMapping
    @Operation(summary = "Get training types")
    public List<TrainingTypeResponse> getTrainingTypes() {
        return mapper.toTrainingTypes(trainingTypeRepository.findAll());
    }
}
