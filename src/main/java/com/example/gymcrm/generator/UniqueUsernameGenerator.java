package com.example.gymcrm.generator;

import com.example.gymcrm.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UniqueUsernameGenerator {
    private final UserRepository userRepository;

    public UniqueUsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generate(String firstName, String lastName) {
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
