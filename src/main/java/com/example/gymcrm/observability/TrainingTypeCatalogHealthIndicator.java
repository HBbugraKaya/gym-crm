package com.example.gymcrm.observability;

import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingTypeCatalogHealthIndicator extends AbstractHealthIndicator {
    private final TrainingTypeRepository trainingTypeRepository;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long available = trainingTypeRepository.count();
        (available == TrainingTypeName.values().length ? builder.up() : builder.down())
                .withDetail("availableTypes", available);
    }
}
