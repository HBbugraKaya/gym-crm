package com.example.gymcrm.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationControllerTest {

    private final AuthenticationController controller = new AuthenticationController();

    @Test
    void loginReturnsOkWhenSecurityContextIsAuthenticated() {
        var response = controller.login();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
