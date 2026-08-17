package com.example.gymcrm.integration.jms;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TraineeDeletionReportPublisherTest {
    @Mock
    private JmsTemplate jmsTemplate;

    private TraineeDeletionReportPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TraineeDeletionReportPublisher(
                jmsTemplate,
                new JmsQueueProperties("gym.trainer.workload", "gym.trainee.deletion-report"));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void publishSendsThePayloadToTheReportQueueWithTransactionId() throws JMSException {
        MDC.put("transactionId", "tx-456");
        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true);

        publisher.publish(request);

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(
                eq("gym.trainee.deletion-report"),
                eq(request),
                postProcessor.capture());

        Message message = mock(Message.class);
        postProcessor.getValue().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-456");
    }

    @Test
    void publishWrapsBrokerFailures() {
        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true);
        doThrow(new UncategorizedJmsException("broker down"))
                .when(jmsTemplate)
                .convertAndSend(eq("gym.trainee.deletion-report"), any(), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> publisher.publish(request))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessage("Trainee deletion report queue is unavailable");
    }
}
