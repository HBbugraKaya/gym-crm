package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials("john.smith", "secret");

    @Mock
    private UserAccountService userAccountService;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private AuthenticationController controller;

    @Test
    void loginAuthenticatesResolvedCredentials() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);

        var response = controller.login(AUTHORIZATION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(userAccountService).authenticate(CREDENTIALS);
    }
}
