package com.example.gymcrm.bdd;

import com.example.gymcrm.integration.jms.JmsQueueProperties;
import io.cucumber.java.Before;
import org.springframework.jms.core.JmsTemplate;

public class BddHooks {
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final ScenarioState state;

    public BddHooks(JmsTemplate jmsTemplate, JmsQueueProperties queues, ScenarioState state) {
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.state = state;
    }

    @Before
    public void resetScenario() {
        state.reset();
        drain(queues.trainerWorkload());
        drain(queues.traineeDeletionReport());
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
