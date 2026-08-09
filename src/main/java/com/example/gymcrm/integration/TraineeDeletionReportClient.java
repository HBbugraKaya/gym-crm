package com.example.gymcrm.integration;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.example.gymcrm.web.filter.TransactionIdFilter.TRANSACTION_ID_HEADER;
import static com.example.gymcrm.web.filter.TransactionIdFilter.TRANSACTION_ID_MDC_KEY;

@Component
@RequiredArgsConstructor
public class TraineeDeletionReportClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeDeletionReportClient.class);

    private final RestClient.Builder restClientBuilder;
    private final ReportServiceProperties properties;

    @CircuitBreaker(name = "reportService", fallbackMethod = "reportFallback")
    public void report(TraineeDeletionReportRequest request) {
        String transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);

        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.serviceUrl() + "/api/v1/trainee-deletion-reports")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentAccessToken())
                    .header(TRANSACTION_ID_HEADER, transactionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info(
                    "Trainee deletion report completed traineeUsername={} transactionId={}",
                    request.traineeUsername(),
                    transactionId);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException("Report service is unavailable", exception);
        }
    }

    private void reportFallback(TraineeDeletionReportRequest request, Throwable exception) {
        LOGGER.warn(
                "Trainee deletion report failed traineeUsername={} failureType={} transactionId={}",
                request.traineeUsername(),
                exception.getClass().getSimpleName(),
                MDC.get(TRANSACTION_ID_MDC_KEY));
        throw new DownstreamServiceException(
                "Report service is temporarily unavailable",
                exception);
    }

    private String currentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new DownstreamServiceException(
                "Authenticated bearer token is unavailable",
                new IllegalStateException("Current authentication is not a JWT"));
    }
}
