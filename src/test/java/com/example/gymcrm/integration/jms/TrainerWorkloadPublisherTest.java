package com.example.gymcrm.integration.jms;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadPublisherTest {
    @Mock
    private JmsTemplate jmsTemplate;

    private TrainerWorkloadPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TrainerWorkloadPublisher(
                jmsTemplate,
                new JmsQueueProperties("gym.trainer.workload", "gym.trainee.deletion-report"));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void publishSendsThePayloadToTheWorkloadQueueWithTransactionId() throws JMSException {
        MDC.put("transactionId", "tx-123");
        TrainerWorkloadRequest request = workloadRequest();

        publisher.publish(request);

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("gym.trainer.workload"), eq(request), postProcessor.capture());

        Message message = mock(Message.class);
        postProcessor.getValue().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-123");
    }

    @Test
    void publishWrapsBrokerFailures() {
        doThrow(new UncategorizedJmsException("broker down"))
                .when(jmsTemplate)
                .convertAndSend(eq("gym.trainer.workload"), any(), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> publisher.publish(workloadRequest()))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessage("Trainer workload queue is unavailable");
    }

    private TrainerWorkloadRequest workloadRequest() {
        return new TrainerWorkloadRequest(
                "coach.one",
                "Coach",
                "One",
                true,
                LocalDate.of(2026, 8, 9),
                60,
                TrainerWorkloadRequest.WorkloadAction.ADD);
    }
}
