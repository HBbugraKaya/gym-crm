package com.example.gymcrm.web.error;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String transactionId
) {
    public static ApiError of(int status, String message, String path) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String error = httpStatus == null ? "HTTP Error" : httpStatus.getReasonPhrase();
        return new ApiError(Instant.now(), status, error, message, path, MDC.get("transactionId"));
    }
}
