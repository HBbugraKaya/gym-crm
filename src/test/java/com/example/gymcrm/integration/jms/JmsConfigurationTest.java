package com.example.gymcrm.integration.jms;

import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.Message;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JmsConfigurationTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void converterUsesJsonTextAndLogicalTypeIds() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MessageConverter converter = new JmsConfiguration().jacksonJmsMessageConverter(objectMapper);
        assertThat(converter).isInstanceOf(MappingJackson2MessageConverter.class);

        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "coach.one",
                "Coach",
                "One",
                true,
                LocalDate.of(2026, 8, 9),
                60,
                TrainerWorkloadRequest.WorkloadAction.ADD);
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText(objectMapper.writeValueAsString(request));
        message.setStringProperty(JmsConfiguration.TYPE_ID_PROPERTY, JmsConfiguration.TRAINER_WORKLOAD_TYPE_ID);

        Object converted = converter.fromMessage(message);
        assertThat(converted).isEqualTo(request);

        TraineeDeletionReportRequest reportRequest = new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true);
        ActiveMQTextMessage reportMessage = new ActiveMQTextMessage();
        reportMessage.setText(objectMapper.writeValueAsString(reportRequest));
        reportMessage.setStringProperty(
                JmsConfiguration.TYPE_ID_PROPERTY,
                JmsConfiguration.TRAINEE_DELETION_REPORT_TYPE_ID);
        assertThat(converter.fromMessage(reportMessage)).isEqualTo(reportRequest);
    }

    @Test
    void transactionIdPostProcessorSkipsMissingMdcValue() throws Exception {
        Message message = mock(Message.class);
        new TransactionIdMessagePostProcessor().postProcessMessage(message);
        verifyNoInteractions(message);
    }

    @Test
    void transactionIdPostProcessorCopiesMdcValue() throws Exception {
        MDC.put("transactionId", "tx-789");
        Message message = mock(Message.class);
        new TransactionIdMessagePostProcessor().postProcessMessage(message);
        verify(message).setStringProperty("transactionId", "tx-789");
    }
}
