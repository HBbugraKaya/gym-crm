package com.example.gymcrm.config;

import org.springframework.boot.artemis.autoconfigure.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsClient;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * JMS and ActiveMQ Artemis configuration for gym-crm.
 */
@Configuration
@EnableJms
public class JmsConfig {

    public static final String TRAINER_WORKLOAD_QUEUE = "trainer.workload.queue";
    public static final String TRAINER_WORKLOAD_DLQ = "trainer.workload.dlq";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsClient jmsClient(JmsTemplate jmsTemplate) {
        return JmsClient.create(jmsTemplate);
    }

    /**
     * Configures the embedded ActiveMQ Artemis server to accept TCP connections on
     * port 61616.
     */
    @Bean
    public ArtemisConfigurationCustomizer artemisConfigurationCustomizer() {
        return configuration -> {
            try {
                configuration.addAcceptorConfiguration("netty-acceptor", "tcp://0.0.0.0:61616");
            } catch (Exception e) {

            }
        };
    }
}
