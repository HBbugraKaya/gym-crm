package com.example.gymcrm.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsClient;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.gymcrm.config.JmsConfig;
import com.example.gymcrm.service.TrainerWorkloadService;
import com.example.gymcrm.utility.MdcUtils;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrainerWorkloadListener {

    private final TrainerWorkloadService workloadService;
    private final JmsClient jmsClient;

    @JmsListener(destination = JmsConfig.TRAINER_WORKLOAD_QUEUE)
    public void receiveWorkload(@Payload TrainerWorkloadRequest request,
            @Header(name = MdcUtils.TRANSACTION_ID, required = false) String transactionId) {

        MdcUtils.setTransactionId(transactionId);

        try {

            log.info("Received workload message from queue for trainer: {}", request.trainerUsername());

            if (isInvalid(request)) {
                sendToDlq(request, "Missing required fields");
                return;
            }

            workloadService.processWorkload(request);
            log.info("Successfully processed workload message for trainer: {}", request.trainerUsername());

        } catch (Exception e) {

            sendToDlq(request, "Processing failed: " + e.getMessage());

        } finally {
            MdcUtils.clear();
        }
    }

    private void sendToDlq(TrainerWorkloadRequest request, String reason) {
        log.warn("Routing message to DLQ. Reason: {}. Payload: {}", reason, request);
        jmsClient.destination(JmsConfig.TRAINER_WORKLOAD_DLQ).send(request);
    }

    private boolean isInvalid(TrainerWorkloadRequest request) {
        return request.trainerUsername() == null || request.trainerUsername().isBlank()
                || request.trainingDate() == null || request.trainingDuration() <= 0
                || request.actionType() == null;
    }
}
