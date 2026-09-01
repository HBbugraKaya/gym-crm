package com.example.gymcrm.report.bdd;

import org.springframework.test.web.servlet.MvcResult;

public class ReportScenarioState {
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
