package com.example.gymcrm.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class TransactionIdFilterTest {

    private static final String INCOMING_ID = "7cc2092d-a293-4f3d-a390-ec12d06d37d8";
    private static final String GENERATED_ID = "6d38d603-c0dc-4149-a2ae-3bb300af8fca";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesValidIncomingTransactionIdAndExposesItDuringTheCall() throws Exception {
        HttpServletRequest request = request(INCOMING_ID);
        HttpServletResponse response = response(200);
        TransactionIdFilter filter = filter();

        filter.doFilterInternal(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isEqualTo(INCOMING_ID));

        verify(response).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, INCOMING_ID);
        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesMissingOrInvalidTransactionIdWithGeneratedUuid() throws Exception {
        HttpServletResponse missingResponse = response(204);
        HttpServletResponse invalidResponse = response(204);
        TransactionIdFilter filter = filter();

        filter.doFilterInternal(request(null), missingResponse, noOpChain());
        filter.doFilterInternal(request("not-a-uuid\r\nInjected"), invalidResponse, noOpChain());

        verify(missingResponse).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, GENERATED_ID);
        verify(invalidResponse).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, GENERATED_ID);
    }

    @Test
    void clearsMdcWhenDownstreamProcessingFails() {
        HttpServletResponse response = response(200);
        TransactionIdFilter filter = filter();

        assertThatThrownBy(() -> filter.doFilterInternal(request(INCOMING_ID), response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("downstream failure");
                }))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcWhenWritingTheResponseHeaderFails() {
        HttpServletResponse response = response(200);
        doThrow(new IllegalStateException("closed response"))
                .when(response).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, INCOMING_ID);

        assertThatThrownBy(() -> filter().doFilterInternal(request(INCOMING_ID), response, noOpChain()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
    }

    private TransactionIdFilter filter() {
        AtomicInteger invocation = new AtomicInteger();
        LongSupplier nanoTime = () -> invocation.getAndIncrement() * 5_000_000L;
        return new TransactionIdFilter(nanoTime, () -> GENERATED_ID);
    }

    private HttpServletRequest request(String transactionId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TransactionIdFilter.TRANSACTION_ID_HEADER)).thenReturn(transactionId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/trainees/John.Smith");
        return request;
    }

    private HttpServletResponse response(int status) {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }

    private FilterChain noOpChain() {
        return (request, response) -> {
        };
    }
}
