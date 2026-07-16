package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Api(tags = "Authentication")
public class AuthenticationController {
    private final UserAccountService userAccountService;
    private final RequestCredentialsResolver credentialsResolver;

    public AuthenticationController(UserAccountService userAccountService,
                                    RequestCredentialsResolver credentialsResolver) {
        this.userAccountService = userAccountService;
        this.credentialsResolver = credentialsResolver;
    }

    @GetMapping("/login")
    @ApiOperation(value = "Log in", notes = "Validates the username and password supplied with HTTP Basic authentication.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Credentials are valid"),
            @ApiResponse(code = 401, message = "Credentials are missing or invalid")
    })
    public ResponseEntity<Void> login(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        userAccountService.authenticate(credentialsResolver.resolve(authorization));
        return ResponseEntity.ok().build();
    }
}
