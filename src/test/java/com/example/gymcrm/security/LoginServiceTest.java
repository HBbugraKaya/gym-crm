package com.example.gymcrm.security;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenService jwtTokenService;

    private LoginService service;

    @BeforeEach
    void setUp() {
        service = new LoginService(authenticationManager, userRepository, jwtTokenService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void loginReturnsAccessTokenAndResetsExpiredLockAfterAuthentication() {
        User user = new User("John", "Smith", "john.smith", "encoded", true);
        user.recordFailedLogin(NOW.minus(Duration.ofMinutes(10)), 3, Duration.ofMinutes(5));
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(userRepository.findByUsernameIgnoreCase("john.smith")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenService.createAccessToken(authentication)).thenReturn("jwt-value");

        String token = service.login("john.smith", "secret");

        assertThat(token).isEqualTo("jwt-value");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(jwtTokenService).createAccessToken(authentication);
    }

    @Test
    void loginRecordsFailedAttemptAndReturnsUnauthorized() {
        User user = new User("John", "Smith", "john.smith", "encoded", true);
        when(userRepository.findByUsernameIgnoreCase("john.smith")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> service.login("john.smith", "wrong"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Invalid username or password");
                });
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void loginRejectsAnAccountWhoseLockHasNotExpired() {
        User user = new User("John", "Smith", "john.smith", "encoded", true);
        for (int attempt = 0; attempt < 3; attempt++) {
            user.recordFailedLogin(NOW, 3, Duration.ofMinutes(5));
        }
        when(userRepository.findByUsernameIgnoreCase("john.smith")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("john.smith", "secret"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.LOCKED));
    }
}
