package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.TrainingTypeService;
import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/training-types")
@Api(tags = "Training types")
public class TrainingTypeController {
    private final TrainingTypeService trainingTypeService;
    private final UserAccountService userAccountService;
    private final GymWebMapper mapper;
    private final RequestCredentialsResolver credentialsResolver;

    public TrainingTypeController(TrainingTypeService trainingTypeService,
                                  UserAccountService userAccountService,
                                  GymWebMapper mapper,
                                  RequestCredentialsResolver credentialsResolver) {
        this.trainingTypeService = trainingTypeService;
        this.userAccountService = userAccountService;
        this.mapper = mapper;
        this.credentialsResolver = credentialsResolver;
    }

    @GetMapping
    @ApiOperation(value = "Get training types",
            notes = "Returns the immutable training-type catalog after trainee or trainer authentication.",
            response = TrainingTypeResponse.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Training types returned"),
            @ApiResponse(code = 401, message = "Credentials are invalid")
    })
    public List<TrainingTypeResponse> getTrainingTypes(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Credentials credentials = credentialsResolver.resolve(authorization);
        userAccountService.authenticate(credentials);
        return mapper.toTrainingTypes(trainingTypeService.findAll());
    }
}
