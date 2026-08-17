package com.example.gymcrm.workload.jms;

import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JmsConfigurationTest {
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

        assertThat(converter.fromMessage(message)).isEqualTo(request);
    }

    @Test
    void connectionFactoryUsesRedeliveryAndLowPrefetch() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost");
        new JmsConfiguration().activeMqCustomizer().customize(factory);

        assertThat(factory.getRedeliveryPolicy().getMaximumRedeliveries()).isEqualTo(3);
        assertThat(factory.getPrefetchPolicy().getQueuePrefetch()).isEqualTo(1);
    }
}
