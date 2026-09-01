package com.example.gymcrm.bdd;

import org.springframework.test.web.servlet.MvcResult;

public class ScenarioState {
    record Account(String username, String password) {
    }

    private String suffix = Long.toUnsignedString(System.nanoTime(), 36);
    private Account trainee;
    private Account secondTrainee;
    private Account trainer;
    private MvcResult lastResponse;
    private Long lastTrainingId;

    void reset() {
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
        trainee = null;
        secondTrainee = null;
        trainer = null;
        lastResponse = null;
        lastTrainingId = null;
    }

    String suffix() {
        return suffix;
    }

    Account trainee() {
        return trainee;
    }

    void setTrainee(Account trainee) {
        this.trainee = trainee;
    }

    Account secondTrainee() {
        return secondTrainee;
    }

    void setSecondTrainee(Account secondTrainee) {
        this.secondTrainee = secondTrainee;
    }

    Account trainer() {
        return trainer;
    }

    void setTrainer(Account trainer) {
        this.trainer = trainer;
    }

    MvcResult lastResponse() {
        return lastResponse;
    }

    void setLastResponse(MvcResult lastResponse) {
        this.lastResponse = lastResponse;
    }

    Long lastTrainingId() {
        return lastTrainingId;
    }

    void setLastTrainingId(Long lastTrainingId) {
        this.lastTrainingId = lastTrainingId;
    }
}
