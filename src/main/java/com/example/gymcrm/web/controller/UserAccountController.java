package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAccountController {
    private final UserAccountService userAccountService;

    @PutMapping("/{username}/password")
    public void changePassword(@PathVariable String username, @RequestBody ChangePasswordRequest request){
        userAccountService.changePassword(username, request.oldPassword(), request.newPassword());
    }

    @GetMapping("/login")
    public void login(@RequestParam String username, @RequestParam String password) {
        if(!userAccountService.matchesCredentials(username, password)) {
            throw new RuntimeException("Invalid credentials");
        }
    }

}
