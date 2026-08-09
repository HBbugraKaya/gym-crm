package com.example.gymcrm.workload.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter extends OncePerRequestFilter {
    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    private static final String TRANSACTION_ID_MDC_KEY = "transactionId";
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String transactionId = resolveTransactionId(request.getHeader(TRANSACTION_ID_HEADER));
        long startedAt = System.nanoTime();
        Throwable failure = null;
        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);

        try {
            response.setHeader(TRANSACTION_ID_HEADER, transactionId);
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            int status = failure != null && response.getStatus() < 400
                    ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    : response.getStatus();
            logCompletion(request, status, elapsedMillis(startedAt), failure);
            MDC.remove(TRANSACTION_ID_MDC_KEY);
        }
    }

    private void logCompletion(
            HttpServletRequest request,
            int status,
            long durationMillis,
            Throwable failure) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String responseMessage = httpStatus == null ? "HTTP Response" : httpStatus.getReasonPhrase();
        String transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);

        if (status >= 500) {
            LOGGER.error(
                    "Workload REST call completed method={} path={} status={} response={} durationMs={} failureType={} transactionId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    responseMessage,
                    durationMillis,
                    failure == null ? "none" : failure.getClass().getSimpleName(),
                    transactionId);
        } else if (status >= 400) {
            LOGGER.warn(
                    "Workload REST call completed method={} path={} status={} response={} durationMs={} transactionId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    responseMessage,
                    durationMillis,
                    transactionId);
        } else {
            LOGGER.info(
                    "Workload REST call completed method={} path={} status={} response={} durationMs={} transactionId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    responseMessage,
                    durationMillis,
                    transactionId);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String resolveTransactionId(String candidate) {
        try {
            return candidate != null && UUID.fromString(candidate).toString().equalsIgnoreCase(candidate)
                    ? candidate
                    : UUID.randomUUID().toString();
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID().toString();
        }
    }
}
