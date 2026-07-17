package com.example.gymcrm.security;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.repository.TraineeRepository;
import com.example.gymcrm.repository.TrainerRepository;
import com.example.gymcrm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TraineeRepository traineeRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @InjectMocks
    private GymUserDetailsService service;

    @Test
    void loadUserByUsernameAssignsTraineeAndTrainerRoles() {
        User user = new User("John", "Smith", "john.smith", "encoded", true);
        when(userRepository.findByUsername("john.smith")).thenReturn(Optional.of(user));
        when(traineeRepository.findByUsername("john.smith")).thenReturn(Optional.of(mockTrainee()));
        when(trainerRepository.findByUsername("john.smith")).thenReturn(Optional.empty());

        var principal = (GymUserPrincipal) service.loadUserByUsername("john.smith");

        assertThat(principal.getUsername()).isEqualTo("john.smith");
        assertThat(principal.getPassword()).isEqualTo("encoded");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_TRAINEE");
    }

    @Test
    void loadUserByUsernameRejectsUsersWithoutProfiles() {
        User user = new User("Ghost", "User", "ghost.user", "encoded", true);
        when(userRepository.findByUsername("ghost.user")).thenReturn(Optional.of(user));
        when(traineeRepository.findByUsername("ghost.user")).thenReturn(Optional.empty());
        when(trainerRepository.findByUsername("ghost.user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost.user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private com.example.gymcrm.domain.Trainee mockTrainee() {
        return new com.example.gymcrm.domain.Trainee(
                new User("John", "Smith", "john.smith", "encoded", true), null, null);
    }
}
