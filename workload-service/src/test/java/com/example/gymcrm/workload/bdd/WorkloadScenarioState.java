package com.example.gymcrm.workload.bdd;

import org.springframework.test.web.servlet.MvcResult;

public class WorkloadScenarioState {
    private MvcResult lastResponse;

    void reset() {
        lastResponse = null;
    }

    MvcResult lastResponse() {
        return lastResponse;
    }

    void setLastResponse(MvcResult lastResponse) {
        this.lastResponse = lastResponse;
    }
}
