package com.example.gymcrm.report.jms;

import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(JmsQueueProperties.class)
public class JmsConfiguration {
    static final String TYPE_ID_PROPERTY = "_type";
    static final String TRAINEE_DELETION_REPORT_TYPE_ID = "TraineeDeletionReportRequest";

    @Bean
    MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName(TYPE_ID_PROPERTY);
        converter.setObjectMapper(objectMapper);
        converter.setTypeIdMappings(Map.of(TRAINEE_DELETION_REPORT_TYPE_ID, TraineeDeletionReportRequest.class));
        return converter;
    }

    @Bean
    ActiveMQConnectionFactoryCustomizer activeMqCustomizer() {
        return factory -> {
            RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
            redeliveryPolicy.setMaximumRedeliveries(3);
            factory.setRedeliveryPolicy(redeliveryPolicy);

            ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
            prefetchPolicy.setQueuePrefetch(1);
            factory.setPrefetchPolicy(prefetchPolicy);
        };
    }
}
