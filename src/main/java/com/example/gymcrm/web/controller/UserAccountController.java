package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import com.example.gymcrm.web.dto.ChangeStatusRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@Api(tags = "User account")
public class UserAccountController {
    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PutMapping("/{username}/password")
    @ApiOperation(value = "Change login password",
            notes = "Uses the authenticated user from HTTP Basic authentication and replaces the password.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Password changed"),
            @ApiResponse(code = 400, message = "Request is invalid or the target username differs"),
            @ApiResponse(code = 401, message = "Current credentials are invalid")
    })
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
            @Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(username, request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{username}/status")
    @ApiOperation(value = "Activate or deactivate a trainee or trainer",
            notes = "Changes the authenticated user's active state. Repeating the current state is rejected.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Status changed"),
            @ApiResponse(code = 400, message = "Request is invalid or the target username differs"),
            @ApiResponse(code = 401, message = "Credentials are invalid"),
            @ApiResponse(code = 409, message = "The requested state is already current")
    })
    public ResponseEntity<Void> changeStatus(
            @PathVariable String username,
            @Valid @RequestBody ChangeStatusRequest request) {
        userAccountService.changeStatus(username, request.active());
        return ResponseEntity.ok().build();
    }
}
