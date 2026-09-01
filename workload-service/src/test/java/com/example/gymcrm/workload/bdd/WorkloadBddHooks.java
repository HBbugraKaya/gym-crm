package com.example.gymcrm.workload.bdd;

import com.example.gymcrm.workload.jms.JmsQueueProperties;
import io.cucumber.java.Before;
import org.springframework.jms.core.JmsTemplate;

public class WorkloadBddHooks {
    private final WorkloadBddConfiguration.WorkloadDocuments documents;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final WorkloadScenarioState state;

    public WorkloadBddHooks(
            WorkloadBddConfiguration.WorkloadDocuments documents,
            JmsTemplate jmsTemplate,
            JmsQueueProperties queues,
            WorkloadScenarioState state) {
        this.documents = documents;
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.state = state;
    }

    @Before
    public void resetScenario() {
        state.reset();
        documents.clear();
        drain(queues.trainerWorkload());
        drain(queues.trainerWorkloadDlq());
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
