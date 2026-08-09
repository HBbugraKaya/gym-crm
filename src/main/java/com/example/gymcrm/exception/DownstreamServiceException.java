package com.example.gymcrm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
