package com.example.gymcrm.integration.jms;

import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    static final String TRAINER_WORKLOAD_TYPE_ID = "TrainerWorkloadRequest";
    static final String TRAINEE_DELETION_REPORT_TYPE_ID = "TraineeDeletionReportRequest";

    @Bean
    MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName(TYPE_ID_PROPERTY);
        converter.setObjectMapper(objectMapper);
        converter.setTypeIdMappings(Map.of(
                TRAINER_WORKLOAD_TYPE_ID, TrainerWorkloadRequest.class,
                TRAINEE_DELETION_REPORT_TYPE_ID, TraineeDeletionReportRequest.class));
        return converter;
    }
}
