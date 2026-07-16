package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.service.command.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountServiceImpl implements UserAccountService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountServiceImpl.class);

    private final UserRepository userRepository;

    public UserAccountServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(Credentials credentials) {
        return userRepository.findByUsername(credentials.username())
                .filter(user -> user.getPassword().equals(credentials.password()))
                .map(user -> {
                    LOGGER.debug("User authenticated username={}", user.getUsername());
                    return user;
                })
                .orElseThrow(() -> {
                    LOGGER.warn("User authentication failed username={}", credentials.username());
                    return new AuthenticationException("User");
                });
    }

    @Override
    @Transactional
    public void changePassword(Credentials credentials, String targetUsername, String newPassword) {
        User user = authenticate(credentials);
        requireOwnAccount(user, targetUsername);
        user.changePassword(newPassword);
        LOGGER.info("Changed user password id={} username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public User changeStatus(Credentials credentials, String targetUsername, boolean active) {
        User user = authenticate(credentials);
        requireOwnAccount(user, targetUsername);
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

    private void requireOwnAccount(User authenticatedUser, String targetUsername) {
        if (!authenticatedUser.getUsername().equalsIgnoreCase(targetUsername)) {
            throw new ValidationException("Authenticated user can only modify own account");
        }
    }
}
