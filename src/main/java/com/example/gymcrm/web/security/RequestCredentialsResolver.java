package com.example.gymcrm.web.security;

import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.service.command.Credentials;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Resolves HTTP Basic credentials without ever logging the authorization value.
 */
@Component
public final class RequestCredentialsResolver {

    private static final String BASIC_SCHEME = "Basic ";
    private static final int MAX_AUTHORIZATION_HEADER_LENGTH = 8_192;

    public Credentials resolve(String authorizationHeader) {
        if (authorizationHeader == null
                || authorizationHeader.length() > MAX_AUTHORIZATION_HEADER_LENGTH
                || !authorizationHeader.regionMatches(true, 0, BASIC_SCHEME, 0, BASIC_SCHEME.length())) {
            throw authenticationFailed();
        }

        String encodedCredentials = authorizationHeader.substring(BASIC_SCHEME.length()).trim();
        if (encodedCredentials.isEmpty()) {
            throw authenticationFailed();
        }

        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(encodedCredentials);
        } catch (IllegalArgumentException exception) {
            throw authenticationFailed();
        }

        try {
            return parse(new String(decodedBytes, StandardCharsets.UTF_8));
        } finally {
            Arrays.fill(decodedBytes, (byte) 0);
        }
    }

    private Credentials parse(String decodedCredentials) {
        int separator = decodedCredentials.indexOf(':');
        if (separator < 1 || separator == decodedCredentials.length() - 1) {
            throw authenticationFailed();
        }

        String username = decodedCredentials.substring(0, separator).trim();
        String password = decodedCredentials.substring(separator + 1);
        if (username.isEmpty() || password.isBlank() || containsLineBreak(username) || containsLineBreak(password)) {
            throw authenticationFailed();
        }
        return new Credentials(username, password);
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private AuthenticationException authenticationFailed() {
        return new AuthenticationException("Basic");
    }
}
