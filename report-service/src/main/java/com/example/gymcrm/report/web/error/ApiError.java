package com.example.gymcrm.report.web.error;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String transactionId) {

    public static ApiError of(int status, String message) {
        return new ApiError(
                Instant.now(),
                status,
                status == 400 ? "Bad Request" : status == 404 ? "Not Found" : "Request Failed",
                message,
                null,
                MDC.get("transactionId"));
    }

    public static ApiError of(int status, String message, String path) {
        return new ApiError(
                Instant.now(),
                status,
                status == 400 ? "Bad Request" : status == 404 ? "Not Found" : "Request Failed",
                message,
                path,
                MDC.get("transactionId"));
    }
}
