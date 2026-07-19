package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService service;

    @Test
    void changePasswordEncodesAndMutatesOnlyAuthenticatedOwnAccount() {
        User user = user("Jane.Doe", "encoded-old", true);
        when(currentUser.requireAuthenticatedUsername()).thenReturn("Jane.Doe");
        when(userRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new");

        service.changePassword("jane.doe", "newPassword");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void changePasswordRejectsDifferentTargetWithoutMutation() {
        User user = user("Jane.Doe", "encoded-old", true);
        when(currentUser.requireAuthenticatedUsername()).thenReturn("Jane.Doe");

        assertThatThrownBy(() -> service.changePassword("Other.User", "newPassword"))
                .isInstanceOf(ValidationException.class);
        assertThat(user.getPassword()).isEqualTo("encoded-old");
    }

    @Test
    void changeStatusUpdatesUserAndRejectsRepeatedState() {
        User activeUser = user("Active.User", "encoded", true);
        when(currentUser.requireAuthenticatedUsername()).thenReturn("Active.User");
        when(userRepository.findByUsername("Active.User")).thenReturn(Optional.of(activeUser));

        assertThat(service.changeStatus("active.user", false)).isSameAs(activeUser);
        assertThat(activeUser.isActive()).isFalse();

        User inactiveUser = user("Inactive.User", "encoded", false);
        when(currentUser.requireAuthenticatedUsername()).thenReturn("Inactive.User");
        when(userRepository.findByUsername("Inactive.User")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.changeStatus("Inactive.User", false))
                .isInstanceOf(ProfileStateException.class)
                .hasMessage("User is already inactive");
    }

    @Test
    void changeStatusRejectsDifferentTargetAndRepeatedActiveState() {
        User user = user("Jane.Doe", "encoded", true);
        when(currentUser.requireAuthenticatedUsername()).thenReturn("Jane.Doe");
        when(userRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changeStatus("Other.User", false))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changeStatus("Jane.Doe", true))
                .isInstanceOf(ProfileStateException.class)
                .hasMessage("User is already active");
        assertThat(user.isActive()).isTrue();
    }

    private User user(String username, String password, boolean active) {
        return new User("First", "Last", username, password, active);
    }
}
