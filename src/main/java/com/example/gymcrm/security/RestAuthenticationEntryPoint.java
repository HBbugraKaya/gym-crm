package com.example.gymcrm.security;

import com.example.gymcrm.web.error.ApiError;
import com.example.gymcrm.web.filter.TransactionIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public final class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String REALM = "Basic realm=\"gym-crm\", charset=\"UTF-8\"";
    private static final String MESSAGE = "Authentication required";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                MESSAGE,
                safePath(request),
                transactionId(),
                List.of());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, REALM);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
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
