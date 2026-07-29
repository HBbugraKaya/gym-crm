package com.example.gymcrm.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GymCrmMetricsTest {

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
    void recordsEverySuccessfulServiceCall() {
        var meterRegistry = new SimpleMeterRegistry();
        var metrics = new GymCrmMetrics(meterRegistry);

        metrics.recordTrainingCreated();

        assertThat(meterRegistry.get("gymcrm.trainings.created").counter().count()).isEqualTo(1.0);
    }
}
