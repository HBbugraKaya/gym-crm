package com.example.gymcrm.messaging;

import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsClient;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.gymcrm.config.JmsConfig;
import com.example.gymcrm.service.TrainerWorkloadService;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Modern JMS Consumer that listens for workload messages from ActiveMQ,
 * validates them, routes invalid ones to DLQ via JmsClient, and processes valid workloads.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrainerWorkloadConsumer {

    private final TrainerWorkloadService workloadService;
    private final JmsClient jmsClient;

    @JmsListener(destination = JmsConfig.TRAINER_WORKLOAD_QUEUE)
    public void receiveWorkload(@Payload TrainerWorkloadRequest request,
                                @Header(name = "X-Transaction-Id", required = false) String transactionId) {

        if (transactionId != null && !transactionId.isBlank()) {
            MDC.put("transactionId", transactionId);
        }

        try {
            log.info("Received workload message from queue for trainer: {}", request.trainerUsername());

            // 1. Validation: Route invalid/missing messages to Dead Letter Queue (DLQ)
            if (request.trainerUsername() == null || request.trainerUsername().isBlank()
                    || request.trainingDate() == null
                    || request.trainingDuration() <= 0
                    || request.actionType() == null) {
                log.warn("Invalid workload message (missing required fields). Routing to DLQ: {}", request);
                jmsClient.destination(JmsConfig.TRAINER_WORKLOAD_DLQ).send(request);
                return;
            }

            // 2. Process valid workload
            workloadService.processWorkload(request);
            log.info("Successfully processed workload message for trainer: {}", request.trainerUsername());

        } catch (Exception e) {
            log.error("Failed to process workload message, sending to DLQ: {}", e.getMessage(), e);
            jmsClient.destination(JmsConfig.TRAINER_WORKLOAD_DLQ).send(request);
        } finally {
            MDC.remove("transactionId");
        }
    }
}
