package com.example.gymcrm.integration.jms;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraineeDeletionReportPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeDeletionReportPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties properties;

    public void publish(TraineeDeletionReportRequest request) {
        try {
            jmsTemplate.convertAndSend(
                    properties.traineeDeletionReport(),
                    request,
                    new TransactionIdMessagePostProcessor());
            LOGGER.info("Published trainee deletion report traineeUsername={}", request.traineeUsername());
        } catch (JmsException exception) {
            LOGGER.error(
                    "Failed to publish trainee deletion report traineeUsername={} failureType={}",
                    request.traineeUsername(),
                    exception.getClass().getSimpleName());
            throw new DownstreamServiceException("Trainee deletion report queue is unavailable", exception);
        }
    }
}
