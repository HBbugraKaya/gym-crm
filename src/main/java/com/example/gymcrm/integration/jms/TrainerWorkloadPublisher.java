package com.example.gymcrm.integration.jms;

import com.example.gymcrm.exception.DownstreamServiceException;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainerWorkloadPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerWorkloadPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties properties;

    public void publish(TrainerWorkloadRequest request) {
        try {
            jmsTemplate.convertAndSend(
                    properties.trainerWorkload(),
                    request,
                    new TransactionIdMessagePostProcessor());
            LOGGER.info(
                    "Published workload message action={} trainerUsername={}",
                    request.action(),
                    request.trainerUsername());
        } catch (JmsException exception) {
            LOGGER.error(
                    "Failed to publish workload message action={} trainerUsername={} failureType={}",
                    request.action(),
                    request.trainerUsername(),
                    exception.getClass().getSimpleName());
            throw new DownstreamServiceException("Trainer workload queue is unavailable", exception);
        }
    }
}
