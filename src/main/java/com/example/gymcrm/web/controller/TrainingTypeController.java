package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import com.example.gymcrm.service.TrainingTypeService;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Tag(name = "Training types", description = "Immutable training-type catalog")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class TrainingTypeController {
    private final TrainingTypeService trainingTypeService;
    private final GymWebMapper mapper;

    public TrainingTypeController(TrainingTypeService trainingTypeService, GymWebMapper mapper) {
        this.trainingTypeService = trainingTypeService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get training types",
            description = "Returns the immutable training-type catalog after authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training types returned"),
            @ApiResponse(responseCode = "401", description = "Credentials are invalid")
    })
    public List<TrainingTypeResponse> getTrainingTypes() {
        return mapper.toTrainingTypes(trainingTypeService.findAll());
    }
}
