package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.TrainingTypeService;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Api(tags = "Training types")
public class TrainingTypeController {
    private final TrainingTypeService trainingTypeService;
    private final GymWebMapper mapper;

    public TrainingTypeController(TrainingTypeService trainingTypeService, GymWebMapper mapper) {
        this.trainingTypeService = trainingTypeService;
        this.mapper = mapper;
    }

    @GetMapping
    @ApiOperation(value = "Get training types",
            notes = "Returns the immutable training-type catalog after authentication.",
            response = TrainingTypeResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Training types returned"),
            @ApiResponse(code = 401, message = "Credentials are invalid")
    })
    public List<TrainingTypeResponse> getTrainingTypes() {
        return mapper.toTrainingTypes(trainingTypeService.findAll());
    }
}
