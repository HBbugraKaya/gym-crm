package com.example.gymcrm.web.controller;

import com.example.gymcrm.security.LoginService;
import com.example.gymcrm.security.TokenRevocationService;
import com.example.gymcrm.web.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationControllerTest {
    private LoginService loginService;
    private TokenRevocationService tokenRevocationService;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        loginService = mock(LoginService.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        controller = new AuthenticationController(loginService, tokenRevocationService);
    }

    @Test
    void loginReturnsBearerAccessToken() {
        when(loginService.login("john.smith", "secret")).thenReturn("jwt-value");

        var response = controller.login(new LoginRequest("john.smith", "secret"));

        assertThat(response.accessToken()).isEqualTo("jwt-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void logoutRevokesTheAuthenticatedToken() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "HS256")
                .subject("john.smith")
                .claim("jti", "token-id")
                .build();

        var response = controller.logout(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(tokenRevocationService).revoke(jwt);
    }
}
