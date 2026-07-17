package com.example.gymcrm.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TransactionIdFilter extends OncePerRequestFilter {

    public static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    public static final String TRANSACTION_ID_MDC_KEY = "transactionId";

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionIdFilter.class);
    private static final int MAX_LOGGABLE_PATH_LENGTH = 2_048;

    private final LongSupplier nanoTime;
    private final Supplier<String> transactionIdGenerator;

    public TransactionIdFilter() {
        this(System::nanoTime, () -> UUID.randomUUID().toString());
    }

    TransactionIdFilter(LongSupplier nanoTime, Supplier<String> transactionIdGenerator) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.transactionIdGenerator = Objects.requireNonNull(transactionIdGenerator, "transactionIdGenerator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String transactionId = resolveTransactionId(request.getHeader(TRANSACTION_ID_HEADER));
        String method = sanitize(request.getMethod());
        String path = sanitize(request.getRequestURI());
        long startedAt = nanoTime.getAsLong();
        Throwable failure = null;

        MDC.put(TRANSACTION_ID_MDC_KEY, transactionId);
        try {
            response.setHeader(TRANSACTION_ID_HEADER, transactionId);
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } catch (Error error) {
            failure = error;
            throw error;
        } finally {
            long durationMillis = Math.max(0L, (nanoTime.getAsLong() - startedAt) / 1_000_000L);
            int status = failure == null || response.getStatus() >= 400
                    ? response.getStatus()
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            logCompletion(method, path, status, durationMillis, failure);
            MDC.remove(TRANSACTION_ID_MDC_KEY);
        }
    }

    private String resolveTransactionId(String candidate) {
        if (candidate != null) {
            String trimmed = candidate.trim();
            if (isCanonicalUuid(trimmed)) {
                return trimmed;
            }
        }
        return transactionIdGenerator.get();
    }

    private boolean isCanonicalUuid(String candidate) {
        if (candidate.length() != 36) {
            return false;
        }
        try {
            return UUID.fromString(candidate).toString().equalsIgnoreCase(candidate);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String sanitize(String value) {
        String safeValue = value == null ? "unknown" : value.replace('\r', '_').replace('\n', '_');
        return safeValue.length() <= MAX_LOGGABLE_PATH_LENGTH
                ? safeValue
                : safeValue.substring(0, MAX_LOGGABLE_PATH_LENGTH);
    }

    private void logCompletion(String method, String path, int status, long durationMillis, Throwable failure) {
        if (status >= 500 || failure != null) {
            String failureType = failure == null ? "none" : failure.getClass().getSimpleName();
            LOGGER.error("REST call completed method={} path={} status={} durationMs={} failureType={}",
                    method, path, status, durationMillis, failureType);
        } else if (status >= 400) {
            LOGGER.warn("REST call completed method={} path={} status={} durationMs={}",
                    method, path, status, durationMillis);
        } else {
            LOGGER.info("REST call completed method={} path={} status={} durationMs={}",
                    method, path, status, durationMillis);
        }
    }
}
