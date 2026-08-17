package com.example.gymcrm.report.jms;

import com.example.gymcrm.report.service.TraineeReportService;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TraineeDeletionReportListenerTest {
    @Mock
    private TraineeReportService traineeReportService;

    @Mock
    private JmsTemplate jmsTemplate;

    private TraineeDeletionReportListener listener;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        listener = new TraineeDeletionReportListener(
                traineeReportService,
                validator,
                jmsTemplate,
                new JmsQueueProperties("gym.trainee.deletion-report", "gym.trainee.deletion-report.dlq"));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void validMessageIsRecorded() {
        TraineeDeletionReportRequest request = validRequest();

        listener.onMessage(request, "tx-1");

        verify(traineeReportService).recordDeletion(request);
        verifyNoInteractions(jmsTemplate);
    }

    @Test
    void invalidMessageIsMovedToDlq() throws JMSException {
        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest("", "", "", true);

        listener.onMessage(request, "tx-2");

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(
                eq("gym.trainee.deletion-report.dlq"),
                eq(request),
                postProcessor.capture());
        verify(traineeReportService, never()).recordDeletion(any());

        Message message = mock(Message.class);
        postProcessor.getValue().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-2");
    }

    @Test
    void unexpectedFailureIsRethrownForRedelivery() {
        TraineeDeletionReportRequest request = validRequest();
        doThrow(new IllegalStateException("boom")).when(traineeReportService).recordDeletion(request);

        assertThatThrownBy(() -> listener.onMessage(request, "tx-3"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jmsTemplate);
    }

    @Test
    void dlqFailureIsRethrown() {
        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest("", "", "", true);
        doThrow(new UncategorizedJmsException("dlq down"))
                .when(jmsTemplate)
                .convertAndSend(eq("gym.trainee.deletion-report.dlq"), eq(request), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> listener.onMessage(request, "tx-4"))
                .isInstanceOf(UncategorizedJmsException.class);
    }

    private TraineeDeletionReportRequest validRequest() {
        return new TraineeDeletionReportRequest("runner.one", "Runner", "One", true);
    }
}
