package com.example.gymcrm.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.gymcrm.entity.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GymUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User dbUser = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role;
        if (traineeRepository.findByUserUsernameIgnoreCase(username).isPresent()) {
            role = "ROLE_TRAINEE";
        } else if (trainerRepository.findByUserUsernameIgnoreCase(username).isPresent()) {
            role = "ROLE_TRAINER";
        } else {
            throw new IllegalStateException("User has no assigned role");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(dbUser.getUsername())
                .password(dbUser.getPassword())
                .disabled(!dbUser.isActive())
                .authorities(role)
                .build();
    }
}
