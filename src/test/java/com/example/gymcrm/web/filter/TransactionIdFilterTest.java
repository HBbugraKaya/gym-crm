package com.example.gymcrm.web.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class TransactionIdFilterTest {

    private static final String INCOMING_ID = "7cc2092d-a293-4f3d-a390-ec12d06d37d8";
    private final Logger logger = (Logger) LoggerFactory.getLogger(TransactionIdFilter.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(logs);
        logs.stop();
        MDC.clear();
    }

    @Test
    void preservesValidIncomingTransactionIdAndExposesItDuringTheCall() throws Exception {
        HttpServletRequest request = request(INCOMING_ID);
        HttpServletResponse response = response(200);
        TransactionIdFilter filter = new TransactionIdFilter();

        filter.doFilterInternal(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isEqualTo(INCOMING_ID));

        verify(response).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, INCOMING_ID);
        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesMissingOrInvalidTransactionIdWithGeneratedUuid() throws Exception {
        HttpServletResponse missingResponse = response(204);
        HttpServletResponse invalidResponse = response(204);
        TransactionIdFilter filter = new TransactionIdFilter();

        filter.doFilterInternal(request(null), missingResponse, noOpChain());
        filter.doFilterInternal(request("not-a-uuid\r\nInjected"), invalidResponse, noOpChain());

        ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);
        verify(missingResponse).setHeader(eq(TransactionIdFilter.TRANSACTION_ID_HEADER), ids.capture());
        verify(invalidResponse).setHeader(eq(TransactionIdFilter.TRANSACTION_ID_HEADER), ids.capture());
        assertThat(ids.getAllValues()).allSatisfy(id ->
                assertThat(UUID.fromString(id).toString()).isEqualTo(id));
    }

    @Test
    void clearsMdcWhenDownstreamProcessingFails() {
        HttpServletResponse response = response(200);
        TransactionIdFilter filter = new TransactionIdFilter();

        assertThatThrownBy(() -> filter.doFilterInternal(request(INCOMING_ID), response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("downstream failure");
                }))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .contains(
                            "status=500",
                            "response=Internal Server Error",
                            "failureType=ServletException");
        });
    }

    @Test
    void logsClientErrorsAtWarnLevel() throws Exception {
        new TransactionIdFilter().doFilterInternal(request(INCOMING_ID), response(400), noOpChain());

        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("status=400", "response=Bad Request");
        });
    }

    @Test
    void clearsMdcWhenWritingTheResponseHeaderFails() {
        HttpServletResponse response = response(200);
        doThrow(new IllegalStateException("closed response"))
                .when(response).setHeader(TransactionIdFilter.TRANSACTION_ID_HEADER, INCOMING_ID);

        assertThatThrownBy(() -> new TransactionIdFilter()
                .doFilterInternal(request(INCOMING_ID), response, noOpChain()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(TransactionIdFilter.TRANSACTION_ID_MDC_KEY)).isNull();
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
