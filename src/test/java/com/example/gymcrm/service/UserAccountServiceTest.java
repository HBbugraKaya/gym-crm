package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService service;

    @Test
    void changePasswordEncodesAndMutatesOnlyAuthenticatedOwnAccount() {
        User user = user("Jane.Doe", "encoded-old", true);
        when(userRepository.findByUsernameIgnoreCase("jane.doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new");

        service.changePassword("jane.doe", "oldPassword", "newPassword");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(passwordEncoder).matches("oldPassword", "encoded-old");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void changePasswordRejectsMissingUserOrIncorrectOldPasswordWithoutMutation() {
        User user = user("Jane.Doe", "encoded-old", true);

        assertThatThrownBy(() -> service.changePassword("Other.User", "oldPassword", "newPassword"))
                .isInstanceOf(EntityNotFoundException.class);

        when(userRepository.findByUsernameIgnoreCase("jane.doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encoded-old")).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword("jane.doe", "wrongPassword", "newPassword"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Old password is incorrect");
                });
        assertThat(user.getPassword()).isEqualTo("encoded-old");
    }

    @Test
    void changeStatusUpdatesUserAndRejectsRepeatedState() {
        User activeUser = user("Active.User", "encoded", true);
        when(userRepository.findByUsernameIgnoreCase("active.user")).thenReturn(Optional.of(activeUser));

        service.changeStatus("active.user", false);
        assertThat(activeUser.isActive()).isFalse();

        User inactiveUser = user("Inactive.User", "encoded", false);
        when(userRepository.findByUsernameIgnoreCase("Inactive.User")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.changeStatus("Inactive.User", false))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("User is already inactive");
                });
    }

    @Test
    void changeStatusRejectsMissingTarget() {
        when(userRepository.findByUsernameIgnoreCase("Other.User")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus("Other.User", false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private User user(String username, String password, boolean active) {
        return new User("First", "Last", username, password, active);
    }
}
