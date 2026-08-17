package com.example.gymcrm.workload.jms;

import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.service.TrainerWorkloadService;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadListenerTest {
    @Mock
    private TrainerWorkloadService trainerWorkloadService;

    @Mock
    private JmsTemplate jmsTemplate;

    private TrainerWorkloadListener listener;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        listener = new TrainerWorkloadListener(
                trainerWorkloadService,
                validator,
                jmsTemplate,
                new JmsQueueProperties("gym.trainer.workload", "gym.trainer.workload.dlq"));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void validMessageIsApplied() {
        TrainerWorkloadRequest request = validRequest();

        listener.onMessage(request, "tx-1");

        verify(trainerWorkloadService).apply(request);
        verifyNoInteractions(jmsTemplate);
    }

    @Test
    void invalidMessageIsMovedToDlq() throws JMSException {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "", "", "", true, null, 0, null);

        listener.onMessage(request, "tx-2");

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("gym.trainer.workload.dlq"), eq(request), postProcessor.capture());
        verify(trainerWorkloadService, never()).apply(any());

        Message message = mock(Message.class);
        postProcessor.getValue().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-2");
    }

    @Test
    void businessValidationFailureIsMovedToDlq() {
        TrainerWorkloadRequest request = validRequest();
        doThrow(new ValidationException("Cannot delete more workload than has been recorded"))
                .when(trainerWorkloadService)
                .apply(request);

        listener.onMessage(request, "tx-3");

        verify(jmsTemplate).convertAndSend(eq("gym.trainer.workload.dlq"), eq(request), any(MessagePostProcessor.class));
    }

    @Test
    void missingTrainerIsMovedToDlqWithoutTransactionId() throws JMSException {
        TrainerWorkloadRequest request = validRequest();
        doThrow(new EntityNotFoundException("No workload exists for trainer coach.one"))
                .when(trainerWorkloadService)
                .apply(request);

        listener.onMessage(request, null);

        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("gym.trainer.workload.dlq"), eq(request), postProcessor.capture());
        Message message = mock(Message.class);
        postProcessor.getValue().postProcessMessage(message);
        verifyNoInteractions(message);
    }

    @Test
    void unexpectedFailureIsRethrownForRedelivery() {
        TrainerWorkloadRequest request = validRequest();
        doThrow(new IllegalStateException("boom")).when(trainerWorkloadService).apply(request);

        assertThatThrownBy(() -> listener.onMessage(request, "tx-4"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jmsTemplate);
    }

    @Test
    void dlqFailureIsRethrown() {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "", "", "", true, null, 0, null);
        doThrow(new UncategorizedJmsException("dlq down"))
                .when(jmsTemplate)
                .convertAndSend(eq("gym.trainer.workload.dlq"), eq(request), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> listener.onMessage(request, "tx-5"))
                .isInstanceOf(UncategorizedJmsException.class);
    }

    private TrainerWorkloadRequest validRequest() {
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
