package com.example.gymcrm.web.error;

import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.ProfileStateException;
import com.example.gymcrm.exception.ValidationException;
import com.example.gymcrm.web.filter.TransactionIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RestControllerAdvice
public final class RestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String GENERIC_SERVER_MESSAGE = "An unexpected server error occurred";
    private static final String VALIDATION_MESSAGE = "Request validation failed";

    private final Clock clock;

    public RestExceptionHandler() {
        this(Clock.systemUTC());
    }

    RestExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"gym-crm\", charset=\"UTF-8\"");
        ApiError body = error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, List.of());
        logClientError(body);
        return new ResponseEntity<>(body, headers, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(
            MissingRequestHeaderException exception, HttpServletRequest request) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(exception.getHeaderName())) {
            return handleAuthentication(new AuthenticationException("Basic"), request);
        }
        return response(HttpStatus.BAD_REQUEST,
                "Required request header is missing: " + exception.getHeaderName(), request, List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoResource(
            Exception exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Resource not found", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this resource",
                request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_ACCEPTABLE, "Requested response type is not supported", request, List.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            EntityNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ProfileStateException.class)
    public ResponseEntity<ApiError> handleConflict(
            ProfileStateException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({
            ValidationException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, validationMessage(exception), request, fieldViolations(exception));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected REST failure path={} exceptionType={}",
                safePath(request), exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_SERVER_MESSAGE, request, List.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String message, HttpServletRequest request, List<FieldViolation> violations) {
        ApiError body = error(status, message, request, violations);
        if (status.is4xxClientError()) {
            logClientError(body);
        }
        return ResponseEntity.status(status).body(body);
    }

    private void logClientError(ApiError error) {
        LOGGER.warn("REST request rejected status={} error={} path={} message={}",
                error.status(), error.error(), error.path(), error.message());
    }

    private ApiError error(
            HttpStatus status, String message, HttpServletRequest request, List<FieldViolation> violations) {
        return new ApiError(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                message,
                safePath(request),
                transactionId(),
                violations
        );
    }

    private String validationMessage(Exception exception) {
        return switch (exception) {
            case ValidationException validation -> validation.getMessage();
            case HttpMessageNotReadableException ignored -> "Malformed request body";
            case MissingServletRequestParameterException missing ->
                    "Required request parameter is missing: " + missing.getParameterName();
            case MethodArgumentTypeMismatchException mismatch ->
                    "Invalid value for request parameter: " + mismatch.getName();
            default -> VALIDATION_MESSAGE;
        };
    }

    private List<FieldViolation> fieldViolations(Exception exception) {
        return switch (exception) {
            case MethodArgumentNotValidException validation -> fromFieldErrors(validation.getBindingResult().getFieldErrors());
            case BindException binding -> fromFieldErrors(binding.getBindingResult().getFieldErrors());
            case ConstraintViolationException validation -> validation.getConstraintViolations().stream()
                    .map(violation -> new FieldViolation(
                            violation.getPropertyPath().toString(), violation.getMessage()))
                    .toList();
            case HandlerMethodValidationException validation -> validation.getAllErrors().stream()
                    .map(error -> new FieldViolation("request", defaultMessage(error.getDefaultMessage())))
                    .toList();
            default -> List.of();
        };
    }

    private List<FieldViolation> fromFieldErrors(List<FieldError> errors) {
        return errors.stream()
                .map(error -> new FieldViolation(error.getField(), defaultMessage(error.getDefaultMessage())))
                .toList();
    }

    private String defaultMessage(String message) {
        return message == null || message.isBlank() ? "Invalid value" : message;
    }

    private String safePath(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null) {
            return "unknown";
        }
        return request.getRequestURI().replace('\r', '_').replace('\n', '_');
    }

    private String transactionId() {
        String transactionId = MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY);
        return transactionId == null ? "unavailable" : transactionId;
    }
}
