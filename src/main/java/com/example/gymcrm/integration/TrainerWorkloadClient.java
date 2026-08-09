package com.example.gymcrm.integration;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
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
public class TrainerWorkloadClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerWorkloadClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "trainerWorkloadService";

    private final RestClient.Builder restClientBuilder;
    private final WorkloadServiceProperties properties;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "synchronizeFallback")
    public void synchronize(TrainerWorkloadRequest request) {
        String transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);
        String accessToken = currentAccessToken();

        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.serviceUrl() + "/api/v1/trainer-workloads")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(TRANSACTION_ID_HEADER, transactionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info(
                    "Downstream workload synchronization completed action={} transactionId={}",
                    request.action(),
                    transactionId);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException(
                    "Trainer workload service is unavailable",
                    exception);
        }
    }

    private void synchronizeFallback(TrainerWorkloadRequest request, Throwable exception) {
        LOGGER.warn(
                "Downstream workload synchronization failed action={} failureType={} transactionId={}",
                request.action(),
                exception.getClass().getSimpleName(),
                MDC.get(TRANSACTION_ID_MDC_KEY));
        throw new DownstreamServiceException(
                "Trainer workload service is temporarily unavailable",
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
