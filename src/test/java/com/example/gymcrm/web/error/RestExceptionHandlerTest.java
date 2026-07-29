package com.example.gymcrm.web.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {
    private final RestExceptionHandler handler = new RestExceptionHandler();

    @AfterEach
    void clearTransactionId() {
        MDC.remove("transactionId");
    }

    @Test
    void responseStatusExceptionCreatesConsistentErrorBody() {
        MDC.put("transactionId", "tx-123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/john.smith/password");

        var response = handler.handleStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.status()).isEqualTo(400);
            assertThat(error.error()).isEqualTo("Bad Request");
            assertThat(error.message()).isEqualTo("Old password is incorrect");
            assertThat(error.path()).isEqualTo("/api/v1/users/john.smith/password");
            assertThat(error.transactionId()).isEqualTo("tx-123");
        });
    }
}
