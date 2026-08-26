package com.example.gymcrm.client;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.jms.core.JmsClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.example.gymcrm.config.JmsConfig;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrainerWorkloadClient {

    private final JmsClient jmsClient;

    public void updateWorkload(TrainerWorkloadRequest request) {
        String transactionId = MDC.get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = UUID.randomUUID().toString();
        }

        log.info("Sending workload message to ActiveMQ queue: {} for trainer: {}",
                JmsConfig.TRAINER_WORKLOAD_QUEUE, request.trainerUsername());

        jmsClient.destination(JmsConfig.TRAINER_WORKLOAD_QUEUE)
                .send(MessageBuilder.withPayload(request)
                        .setHeader("X-Transaction-Id", transactionId)
                        .build());
    }
}
