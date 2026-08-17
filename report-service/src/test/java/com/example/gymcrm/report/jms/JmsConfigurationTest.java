package com.example.gymcrm.report.jms;

import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class JmsConfigurationTest {
    @Test
    void converterUsesJsonTextAndLogicalTypeIds() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageConverter converter = new JmsConfiguration().jacksonJmsMessageConverter(objectMapper);
        assertThat(converter).isInstanceOf(MappingJackson2MessageConverter.class);

        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true);
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText(objectMapper.writeValueAsString(request));
        message.setStringProperty(
                JmsConfiguration.TYPE_ID_PROPERTY,
                JmsConfiguration.TRAINEE_DELETION_REPORT_TYPE_ID);

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
