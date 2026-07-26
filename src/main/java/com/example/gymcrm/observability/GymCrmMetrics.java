package com.example.gymcrm.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GymCrmMetrics {
    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter trainingsCreated;

    public GymCrmMetrics(MeterRegistry meterRegistry) {
        traineeRegistrations = meterRegistry.counter("gymcrm.profiles.created", "type", "trainee");
        trainerRegistrations = meterRegistry.counter("gymcrm.profiles.created", "type", "trainer");
        trainingsCreated = meterRegistry.counter("gymcrm.trainings.created");
    }

    public void recordTraineeRegistration() {
        traineeRegistrations.increment();
    }

    public void recordTrainerRegistration() {
        trainerRegistrations.increment();
    }

    public void recordTrainingCreated() {
        trainingsCreated.increment();
    }
}
