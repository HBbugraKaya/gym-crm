package com.example.gymcrm.report.jms;

import com.example.gymcrm.report.service.TraineeReportService;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
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
public class TraineeDeletionReportListener {
    static final String TRANSACTION_ID_PROPERTY = "transactionId";

    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeDeletionReportListener.class);

    private final TraineeReportService traineeReportService;
    private final Validator validator;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties properties;

    @JmsListener(destination = "${gymcrm.jms.queues.trainee-deletion-report}")
    public void onMessage(
            @Payload TraineeDeletionReportRequest request,
            @Header(name = TRANSACTION_ID_PROPERTY, required = false) String transactionId) {
        if (transactionId != null) {
            MDC.put(TRANSACTION_ID_PROPERTY, transactionId);
        }
        LOGGER.info(
                "Started trainee deletion report transaction traineeUsername={} transactionIdPresent={}",
                request.traineeUsername(),
                transactionId != null);
        try {
            Set<ConstraintViolation<TraineeDeletionReportRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                LOGGER.warn(
                        "Rejected invalid trainee deletion report traineeUsername={} violationCount={}",
                        request.traineeUsername(),
                        violations.size());
                sendToDeadLetterQueue(request, transactionId);
                return;
            }

            traineeReportService.recordDeletion(request);
            LOGGER.info("Processed trainee deletion report traineeUsername={}", request.traineeUsername());
        } finally {
            MDC.remove(TRANSACTION_ID_PROPERTY);
        }
    }

    private void sendToDeadLetterQueue(TraineeDeletionReportRequest request, String transactionId) {
        jmsTemplate.convertAndSend(properties.traineeDeletionReportDlq(), request, message -> {
            if (transactionId != null) {
                message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
            }
            return message;
        });
        LOGGER.warn(
                "Moved invalid trainee deletion report to DLQ queue={} traineeUsername={}",
                properties.traineeDeletionReportDlq(),
                request.traineeUsername());
    }
}
