package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                              CurrentUser currentUser,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String targetUsername, String newPassword) {
        String authenticatedUsername = currentUser.requireAuthenticatedUsername();
        requireOwnAccount(authenticatedUsername, targetUsername);
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ValidationException("Authenticated user can only modify own account"));
        user.changePassword(passwordEncoder.encode(newPassword));
        LOGGER.info("Changed user password id={} username={}", user.getId(), user.getUsername());
    }

    @Transactional
    public User changeStatus(String targetUsername, boolean active) {
        String authenticatedUsername = currentUser.requireAuthenticatedUsername();
        requireOwnAccount(authenticatedUsername, targetUsername);
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ValidationException("Authenticated user can only modify own account"));
        if (user.isActive() == active) {
            LOGGER.warn("User status change rejected id={} username={} active={}",
                    user.getId(), user.getUsername(), active);
            throw new ProfileStateException("User is already " + (active ? "active" : "inactive"));
        }
        user.setActive(active);
        LOGGER.info("Changed user status id={} username={} active={}",
                user.getId(), user.getUsername(), active);
        return user;
    }

    private void requireOwnAccount(String authenticatedUsername, String targetUsername) {
        SelfAccess.require(authenticatedUsername, targetUsername,
                "Authenticated user can only modify own account");
    }
}
