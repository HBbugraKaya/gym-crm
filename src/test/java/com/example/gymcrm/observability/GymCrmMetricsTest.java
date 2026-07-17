package com.example.gymcrm.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

class GymCrmMetricsTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void recordsLowCardinalityProfileAndTrainingCounters() {
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new GymCrmMetrics(meterRegistry);

        metrics.recordTraineeRegistration();
        metrics.recordTrainerRegistration();
        metrics.recordTrainingCreated();

        assertThat(meterRegistry.get("gymcrm.profiles.created").tag("type", "trainee").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("gymcrm.profiles.created").tag("type", "trainer").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("gymcrm.trainings.created").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordsCountersOnlyAfterTransactionCommit() {
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new GymCrmMetrics(meterRegistry);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        metrics.recordTrainingCreated();

        assertThat(meterRegistry.get("gymcrm.trainings.created").counter().count()).isZero();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(meterRegistry.get("gymcrm.trainings.created").counter().count()).isEqualTo(1.0);
    }
}
