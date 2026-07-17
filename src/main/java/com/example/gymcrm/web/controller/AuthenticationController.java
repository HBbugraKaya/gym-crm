package com.example.gymcrm.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Api(tags = "Authentication")
public class AuthenticationController {

    @GetMapping("/login")
    @ApiOperation(value = "Log in", notes = "Returns success when HTTP Basic credentials are valid.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Credentials are valid"),
            @ApiResponse(code = 401, message = "Credentials are missing or invalid")
    })
    public ResponseEntity<Void> login() {
        return ResponseEntity.ok().build();
    }
}
