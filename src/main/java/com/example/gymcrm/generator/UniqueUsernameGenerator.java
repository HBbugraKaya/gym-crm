package com.example.gymcrm.generator;

import com.example.gymcrm.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UniqueUsernameGenerator implements UsernameGenerator {
    private final UserRepository userRepository;

    public UniqueUsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String generate(String firstName, String lastName) {
        String trimmedFirstName = firstName.trim();
        String trimmedLastName = lastName.trim();
        String base = trimmedFirstName + "." + trimmedLastName;
        long suffix = userRepository.countByFirstNameAndLastName(trimmedFirstName, trimmedLastName);

        String candidate = suffix == 0 ? base : base + suffix;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
