package com.example.gymcrm.workload.jms;

import com.example.gymcrm.workload.exception.EntityNotFoundException;
import com.example.gymcrm.workload.exception.ValidationException;
import com.example.gymcrm.workload.service.TrainerWorkloadService;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainerWorkloadListener {
    static final String TRANSACTION_ID_PROPERTY = "transactionId";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainerWorkloadListener.class);

    private final TrainerWorkloadService trainerWorkloadService;
    private final Validator validator;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties properties;

    @JmsListener(destination = "${gymcrm.jms.queues.trainer-workload}")
    public void onMessage(
            @Payload TrainerWorkloadRequest request,
            @Header(name = TRANSACTION_ID_PROPERTY, required = false) String transactionId) {
        if (transactionId != null) {
            MDC.put(TRANSACTION_ID_PROPERTY, transactionId);
        }
        LOGGER.info(
                "Started workload transaction trainerUsername={} transactionIdPresent={}",
                request.trainerUsername(),
                transactionId != null);
        try {
            Set<ConstraintViolation<TrainerWorkloadRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                LOGGER.warn(
                        "Rejected invalid workload message trainerUsername={} violationCount={}",
                        request.trainerUsername(),
                        violations.size());
                sendToDeadLetterQueue(request, transactionId);
                return;
            }

            trainerWorkloadService.apply(request);
            LOGGER.info(
                    "Completed workload transaction action={} trainerUsername={}",
                    request.action(),
                    request.trainerUsername());
        } catch (ValidationException | EntityNotFoundException exception) {
            LOGGER.warn(
                    "Rejected workload message trainerUsername={} reason={}",
                    request.trainerUsername(),
                    exception.getMessage());
            sendToDeadLetterQueue(request, transactionId);
        } finally {
            LOGGER.info(
                    "Finished workload transaction trainerUsername={}",
                    request.trainerUsername());
            MDC.remove(TRANSACTION_ID_PROPERTY);
        }
    }

    private void sendToDeadLetterQueue(TrainerWorkloadRequest request, String transactionId) {
        jmsTemplate.convertAndSend(properties.trainerWorkloadDlq(), request, message -> {
            if (transactionId != null) {
                message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
            }
            return message;
        });
        LOGGER.warn(
                "Moved invalid workload message to DLQ queue={} trainerUsername={}",
                properties.trainerWorkloadDlq(),
                request.trainerUsername());
    }
}
