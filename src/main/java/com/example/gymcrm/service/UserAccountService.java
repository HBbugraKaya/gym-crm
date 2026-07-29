package com.example.gymcrm.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gymcrm.entity.User;
import com.example.gymcrm.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;

    public boolean matchesCredentials(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getPassword().equals(password);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("Old password is incorrect");
        }
        user.setPassword(newPassword);
    }
}
