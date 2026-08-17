package com.example.gymcrm.integration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.MDC;
import org.springframework.jms.core.MessagePostProcessor;

import static com.example.gymcrm.web.filter.TransactionIdFilter.TRANSACTION_ID_MDC_KEY;

final class TransactionIdMessagePostProcessor implements MessagePostProcessor {
    static final String TRANSACTION_ID_PROPERTY = "transactionId";

    @Override
    public Message postProcessMessage(Message message) throws JMSException {
        String transactionId = MDC.get(TRANSACTION_ID_MDC_KEY);
        if (transactionId != null) {
            message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
        }
        return message;
    }
}
