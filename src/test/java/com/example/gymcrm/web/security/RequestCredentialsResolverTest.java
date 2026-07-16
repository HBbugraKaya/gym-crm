package com.example.gymcrm.web.security;

import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.service.command.Credentials;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCredentialsResolverTest {

    private final RequestCredentialsResolver resolver = new RequestCredentialsResolver();

    @Test
    void resolvesCaseInsensitiveBasicSchemeAndKeepsColonInPassword() {
        String header = basicHeader("  John.Smith:secret:part  ").replace("Basic", "bAsIc");

        Credentials credentials = resolver.resolve(header);

        assertThat(credentials.username()).isEqualTo("John.Smith");
        assertThat(credentials.password()).isEqualTo("secret:part  ");
    }

    @Test
    void rejectsMissingUnsupportedAndMalformedHeadersWithoutExposingTheirValue() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Basic authentication failed");
        assertThatThrownBy(() -> resolver.resolve("Bearer secret-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Basic authentication failed");
        assertThatThrownBy(() -> resolver.resolve("Basic not-base64%%%"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Basic authentication failed");
    }

    @Test
    void rejectsCredentialsWithoutBothUsernameAndPassword() {
        assertThatThrownBy(() -> resolver.resolve(basicHeader(":password")))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> resolver.resolve(basicHeader("username:")))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> resolver.resolve(basicHeader("username:   ")))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void rejectsLineBreaksToPreventLogAndHeaderInjection() {
        assertThatThrownBy(() -> resolver.resolve(basicHeader("username:secret\r\nvalue")))
                .isInstanceOf(AuthenticationException.class);
    }

    private String basicHeader(String value) {
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
