package com.example.gymcrm.web.error;

import com.example.gymcrm.exception.EntityNotFoundException;
import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({ValidationException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .findFirst()
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<ApiError> handleDownstreamFailure(
            DownstreamServiceException exception,
            HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ApiError> handleAccessDenied(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access is denied", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error(
                "Unhandled REST request failure path={} failureType={}",
                request.getRequestURI(),
                exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be processed", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        LOGGER.debug(
                "REST error status={} path={} reasonPresent={}",
                status.value(),
                request.getRequestURI(),
                message != null);
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message, request.getRequestURI()));
    }

    private String fieldMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
