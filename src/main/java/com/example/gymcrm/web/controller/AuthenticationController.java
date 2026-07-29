package com.example.gymcrm.web.controller;

import com.example.gymcrm.security.LoginService;
import com.example.gymcrm.security.TokenRevocationService;
import com.example.gymcrm.web.OpenApiConfig;
import com.example.gymcrm.web.dto.LoginRequest;
import com.example.gymcrm.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT login and logout")
@RequiredArgsConstructor
public class AuthenticationController {
    private final LoginService loginService;
    private final TokenRevocationService tokenRevocationService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username and password")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(loginService.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current JWT")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        tokenRevocationService.revoke(jwt);
        return ResponseEntity.noContent().build();
    }
}
