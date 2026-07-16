package com.example.gymcrm.service.impl;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.repository.UserRepository;
import com.example.gymcrm.service.command.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccountServiceImpl service;

    @Test
    void authenticateSupportsAnyUserRole() {
        User user = user("Any.Role", "secret1234", true);
        when(userRepository.findByUsername("Any.Role")).thenReturn(Optional.of(user));

        assertThat(service.authenticate(new Credentials("Any.Role", "secret1234"))).isSameAs(user);
        verify(userRepository).findByUsername("Any.Role");
    }

    @Test
    void authenticateRejectsMissingUserAndWrongPassword() {
        User user = user("Known.User", "secret1234", true);
        when(userRepository.findByUsername("Known.User")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("Missing.User")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(new Credentials("Known.User", "wrong")))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> service.authenticate(new Credentials("Missing.User", "secret1234")))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void changePasswordMutatesOnlyAuthenticatedOwnAccount() {
        User user = user("Jane.Doe", "oldPassword", true);
        Credentials credentials = new Credentials("Jane.Doe", "oldPassword");
        when(userRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));

        service.changePassword(credentials, "jane.doe", "newPassword");

        assertThat(user.getPassword()).isEqualTo("newPassword");
        assertThat(user.getUsername()).isEqualTo("Jane.Doe");
    }

    @Test
    void changePasswordRejectsDifferentTargetWithoutMutation() {
        User user = user("Jane.Doe", "oldPassword", true);
        Credentials credentials = new Credentials("Jane.Doe", "oldPassword");
        when(userRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(credentials, "Other.User", "newPassword"))
                .isInstanceOf(ValidationException.class);
        assertThat(user.getPassword()).isEqualTo("oldPassword");
    }

    @Test
    void changeStatusUpdatesUserAndRejectsRepeatedState() {
        User activeUser = user("Active.User", "secret1234", true);
        Credentials activeCredentials = new Credentials("Active.User", "secret1234");
        when(userRepository.findByUsername("Active.User")).thenReturn(Optional.of(activeUser));

        assertThat(service.changeStatus(activeCredentials, "active.user", false)).isSameAs(activeUser);
        assertThat(activeUser.isActive()).isFalse();

        User inactiveUser = user("Inactive.User", "secret1234", false);
        Credentials inactiveCredentials = new Credentials("Inactive.User", "secret1234");
        when(userRepository.findByUsername("Inactive.User")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.changeStatus(inactiveCredentials, "Inactive.User", false))
                .isInstanceOf(ProfileStateException.class)
                .hasMessage("User is already inactive");
    }

    @Test
    void changeStatusRejectsDifferentTargetAndRepeatedActiveState() {
        User user = user("Jane.Doe", "secret1234", true);
        Credentials credentials = new Credentials("Jane.Doe", "secret1234");
        when(userRepository.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changeStatus(credentials, "Other.User", false))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changeStatus(credentials, "Jane.Doe", true))
                .isInstanceOf(ProfileStateException.class)
                .hasMessage("User is already active");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void credentialsToStringNeverExposesPassword() {
        Credentials credentials = new Credentials("Jane.Doe", "top-secret-password");

        assertThat(credentials.toString())
                .contains("Jane.Doe", "[REDACTED]")
                .doesNotContain("top-secret-password");
    }

    private User user(String username, String password, boolean active) {
        return new User("First", "Last", username, password, active);
    }
}
