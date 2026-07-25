package com.example.gymcrm.utility;

import org.springframework.stereotype.Component;

import com.example.gymcrm.repository.UserRepository;

@Component
public class UsernameGenerator {

    private final UserRepository userRepository;

    public UsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUsername(String firstName, String lastName) {
        String base = firstName.trim() + "." + lastName.trim();
        String candidate = base;
        int suffix = 1;

        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }
}
