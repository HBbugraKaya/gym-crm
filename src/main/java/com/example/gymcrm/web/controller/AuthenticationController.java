package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "HTTP Basic authentication verification")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class AuthenticationController {

    @GetMapping("/login")
    @Operation(summary = "Log in", description = "Returns success when HTTP Basic credentials are valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials are valid"),
            @ApiResponse(responseCode = "401", description = "Credentials are missing or invalid")
    })
    public ResponseEntity<Void> login() {
        return ResponseEntity.ok().build();
    }
}
