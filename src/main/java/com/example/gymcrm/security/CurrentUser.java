package com.example.gymcrm.security;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CurrentUser {

    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;

    public CurrentUser(TraineeRepository traineeRepository, TrainerRepository trainerRepository) {
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
    }

    public String requireAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationException("User");
        }
        return authentication.getName();
    }

    public boolean hasRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    @Transactional(readOnly = true)
    public Trainee requireTrainee() {
        requireRole("TRAINEE");
        return traineeRepository.findByUsername(requireAuthenticatedUsername())
                .orElseThrow(() -> new AuthenticationException("Trainee"));
    }

    @Transactional(readOnly = true)
    public Trainer requireTrainer() {
        requireRole("TRAINER");
        return trainerRepository.findByUsername(requireAuthenticatedUsername())
                .orElseThrow(() -> new AuthenticationException("Trainer"));
    }

    private void requireRole(String role) {
        if (!hasRole(role)) {
            String profileType = switch (role) {
                case "TRAINEE" -> "Trainee";
                case "TRAINER" -> "Trainer";
                default -> "User";
            };
            throw new AuthenticationException(profileType);
        }
    }
}
