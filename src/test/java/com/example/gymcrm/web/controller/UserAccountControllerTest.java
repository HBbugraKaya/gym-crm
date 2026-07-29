package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.web.dto.ChangePasswordRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountControllerTest {
    private static final String USERNAME = "john.smith";

    @Mock
    private UserAccountService userAccountService;
    @InjectMocks
    private UserAccountController controller;

    @Test
    void changePasswordDelegatesToCommonUserService() {
        var response = controller.changePassword(USERNAME, new ChangePasswordRequest("old-secret", "new-secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccountService).changePassword(USERNAME, "old-secret", "new-secret");
    }
}
