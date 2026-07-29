package com.example.gymcrm.security;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String login(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        Instant now = clock.instant();
        if (user != null && user.isLockedAt(now)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is temporarily locked");
        }
        if (user != null) {
            user.clearExpiredLock(now);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            if (user != null) {
                user.resetFailedLoginAttempts();
            }
            return jwtTokenService.createAccessToken(authentication);
        } catch (AuthenticationException exception) {
            if (user != null) {
                user.recordFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCK_DURATION);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }
}
