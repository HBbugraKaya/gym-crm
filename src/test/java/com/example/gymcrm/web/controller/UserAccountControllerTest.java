package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import com.example.gymcrm.web.dto.ChangeStatusRequest;
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
class UserAccountControllerTest {
    private static final String USERNAME = "john.smith";
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials(USERNAME, "old-secret");

    @Mock
    private UserAccountService userAccountService;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private UserAccountController controller;

    @Test
    void changePasswordDelegatesToCommonUserService() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);

        var response = controller.changePassword(
                USERNAME, AUTHORIZATION, new ChangePasswordRequest("new-secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(userAccountService).changePassword(CREDENTIALS, USERNAME, "new-secret");
    }

    @Test
    void changeStatusDelegatesToCommonUserService() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);

        var response = controller.changeStatus(USERNAME, AUTHORIZATION, new ChangeStatusRequest(false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(userAccountService).changeStatus(CREDENTIALS, USERNAME, false);
    }
}
