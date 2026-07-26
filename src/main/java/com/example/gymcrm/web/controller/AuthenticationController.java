package com.example.gymcrm.web.controller;

import com.example.gymcrm.web.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Verify HTTP Basic credentials")
    public ResponseEntity<Void> login() {
        return ResponseEntity.ok().build();
    }
}
