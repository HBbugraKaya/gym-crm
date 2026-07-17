package com.example.gymcrm.security;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserTest {
    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @InjectMocks
    private CurrentUser currentUser;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireAuthenticatedUsernameReturnsPrincipalName() {
        authenticate("john.smith", List.of(new SimpleGrantedAuthority("ROLE_TRAINEE")));

        assertThat(currentUser.requireAuthenticatedUsername()).isEqualTo("john.smith");
    }

    @Test
    void requireAuthenticatedUsernameRejectsMissingAuthentication() {
        assertThatThrownBy(() -> currentUser.requireAuthenticatedUsername())
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void requireTraineeLoadsProfileWhenRolePresent() {
        Trainee trainee = new Trainee(
                new User("John", "Smith", "john.smith", "encoded", true), null, null);
        authenticate("john.smith", List.of(new SimpleGrantedAuthority("ROLE_TRAINEE")));
        when(traineeRepository.findByUsername("john.smith")).thenReturn(Optional.of(trainee));

        assertThat(currentUser.requireTrainee()).isSameAs(trainee);
    }

    @Test
    void requireTrainerRejectsMissingRole() {
        authenticate("john.smith", List.of(new SimpleGrantedAuthority("ROLE_TRAINEE")));

        assertThatThrownBy(() -> currentUser.requireTrainer())
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void hasRoleChecksGrantedAuthorities() {
        authenticate("alice.coach", List.of(new SimpleGrantedAuthority("ROLE_TRAINER")));

        assertThat(currentUser.hasRole("TRAINER")).isTrue();
        assertThat(currentUser.hasRole("TRAINEE")).isFalse();
    }

    private void authenticate(String username, List<SimpleGrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "secret", authorities));
    }
}
