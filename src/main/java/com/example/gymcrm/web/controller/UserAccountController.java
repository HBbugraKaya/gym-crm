package com.example.gymcrm.web.controller;

import com.example.gymcrm.config.OpenApiConfig;
import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import com.example.gymcrm.web.dto.ChangeStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User account", description = "Authenticated account management")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class UserAccountController {
    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PutMapping("/{username}/password")
    @Operation(summary = "Change login password",
            description = "Uses the authenticated user from HTTP Basic authentication and replaces the password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Request is invalid or the target username differs"),
            @ApiResponse(responseCode = "401", description = "Current credentials are invalid")
    })
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
            @Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(username, request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or deactivate a trainee or trainer",
            description = "Changes the authenticated user's active state. Repeating the current state is rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Request is invalid or the target username differs"),
            @ApiResponse(responseCode = "401", description = "Credentials are invalid"),
            @ApiResponse(responseCode = "409", description = "The requested state is already current")
    })
    public ResponseEntity<Void> changeStatus(
            @PathVariable String username,
            @Valid @RequestBody ChangeStatusRequest request) {
        userAccountService.changeStatus(username, request.active());
        return ResponseEntity.ok().build();
    }
}
