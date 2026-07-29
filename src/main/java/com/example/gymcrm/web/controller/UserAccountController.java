package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.OpenApiConfig;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User account", description = "Authenticated account management")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
@RequiredArgsConstructor
public class UserAccountController {
    private final UserAccountService userAccountService;

    @PutMapping("/{username}/password")
    @Operation(
            summary = "Change password",
            description = "The current username is supplied through the Bearer JWT")
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
        @Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(username, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
