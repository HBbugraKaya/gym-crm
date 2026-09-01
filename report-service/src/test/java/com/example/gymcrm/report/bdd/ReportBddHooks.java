package com.example.gymcrm.report.bdd;

import com.example.gymcrm.report.jms.JmsQueueProperties;
import io.cucumber.java.Before;
import org.springframework.jms.core.JmsTemplate;

public class ReportBddHooks {
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final ReportScenarioState state;

    public ReportBddHooks(JmsTemplate jmsTemplate, JmsQueueProperties queues, ReportScenarioState state) {
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.state = state;
    }

    @Before
    public void resetScenario() {
        state.reset();
        drain(queues.traineeDeletionReport());
        drain(queues.traineeDeletionReportDlq());
    }

    private void drain(String destination) {
        long previousTimeout = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(50);
        try {
            while (jmsTemplate.receive(destination) != null) {
                // discard leftover messages from earlier scenarios
            }
        } finally {
            jmsTemplate.setReceiveTimeout(previousTimeout);
        }
    }
}
