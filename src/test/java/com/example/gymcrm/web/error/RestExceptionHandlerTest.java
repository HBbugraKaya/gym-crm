package com.example.gymcrm.web.error;

import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.web.filter.TransactionIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");
    private static final String TRANSACTION_ID = "7cc2092d-a293-4f3d-a390-ec12d06d37d8";

    private RestExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/trainees/John.Smith");
        MDC.put(TransactionIdFilter.TRANSACTION_ID_MDC_KEY, TRANSACTION_ID);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mapsAuthenticationFailureToUnauthorizedWithBasicChallenge() {
        var response = handler.handleAuthentication(new AuthenticationException("Trainee"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).contains("Basic");
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.timestamp()).isEqualTo(NOW);
            assertThat(error.status()).isEqualTo(401);
            assertThat(error.path()).isEqualTo("/api/v1/trainees/John.Smith");
            assertThat(error.transactionId()).isEqualTo(TRANSACTION_ID);
        });
    }

    @Test
    void mapsDomainExceptionsToDocumentedStatuses() {
        assertThat(handler.handleNotFound(new EntityNotFoundException("Trainee", "missing"), request)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleConflict(new ProfileStateException("Already active"), request)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleBadRequest(new ValidationException("firstName is required"), request)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsHttpProtocolExceptionsWithoutStartingSpring() {
        var unsupportedMediaType = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        assertThat(handler.handleNoResource(new Exception("missing"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("POST"), request).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(handler.handleUnsupportedMediaType(unsupportedMediaType, request).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void returnsFieldErrorsForBindingFailuresWithoutRejectedValues() {
        BindException binding = new BindException(new Object(), "request");
        binding.addError(new FieldError("request", "firstName", "private rejected value",
                false, null, null, "must not be blank"));

        var response = handler.handleBadRequest(binding, request);

        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.message()).isEqualTo("Request validation failed");
            assertThat(error.fieldErrors())
                    .containsExactly(new FieldViolation("firstName", "must not be blank"));
            assertThat(error.toString()).doesNotContain("private rejected value");
        });
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        var response = handler.handleUnexpected(new IllegalStateException("password=very-secret"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.message()).isEqualTo("An unexpected server error occurred");
            assertThat(error.toString()).doesNotContain("very-secret");
        });
    }

    @Test
    void errorModelDefensivelyCopiesFieldErrors() {
        var violations = new java.util.ArrayList<FieldViolation>();
        ApiError error = new ApiError(NOW, 400, "Bad Request", "Invalid", "/path",
                TRANSACTION_ID, violations);

        violations.add(new FieldViolation("field", "message"));

        assertThat(error.fieldErrors()).isEmpty();
    }
}
