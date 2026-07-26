package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @PreAuthorize("#targetUsername.equalsIgnoreCase(authentication.name)")
    public void changePassword(String targetUsername, String newPassword) {
        User user = find(targetUsername);
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    @PreAuthorize("#targetUsername.equalsIgnoreCase(authentication.name)")
    public void changeStatus(String targetUsername, boolean active) {
        User user = find(targetUsername);
        if (user.isActive() == active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is already " + (active ? "active" : "inactive"));
        }
        user.setActive(active);
    }

    private User find(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("User", username));
    }
}
