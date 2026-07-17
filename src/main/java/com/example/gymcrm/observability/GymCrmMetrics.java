package com.example.gymcrm.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class GymCrmMetrics {
    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter trainingsCreated;

    public GymCrmMetrics(MeterRegistry meterRegistry) {
        traineeRegistrations = profileRegistrationCounter(meterRegistry, "trainee");
        trainerRegistrations = profileRegistrationCounter(meterRegistry, "trainer");
        trainingsCreated = Counter.builder("gymcrm.trainings.created")
                .description("Number of trainings created successfully")
                .register(meterRegistry);
    }

    public void recordTraineeRegistration() {
        incrementAfterCommit(traineeRegistrations);
    }

    public void recordTrainerRegistration() {
        incrementAfterCommit(trainerRegistrations);
    }

    public void recordTrainingCreated() {
        incrementAfterCommit(trainingsCreated);
    }

    private void incrementAfterCommit(Counter counter) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            counter.increment();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                counter.increment();
            }
        });
    }

    private Counter profileRegistrationCounter(MeterRegistry meterRegistry, String profileType) {
        return Counter.builder("gymcrm.profiles.created")
                .description("Number of gym profiles created successfully")
                .tag("type", profileType)
                .register(meterRegistry);
    }
}
