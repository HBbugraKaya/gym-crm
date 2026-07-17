package com.example.gymcrm.observability;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.TrainingTypeService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component("trainingTypeCatalogHealthIndicator")
public class TrainingTypeCatalogHealthIndicator implements HealthIndicator {
    private final TrainingTypeService trainingTypeService;

    public TrainingTypeCatalogHealthIndicator(TrainingTypeService trainingTypeService) {
        this.trainingTypeService = trainingTypeService;
    }

    @Override
    public Health health() {
        try {
            Set<TrainingTypeName> expected = EnumSet.allOf(TrainingTypeName.class);
            Set<TrainingTypeName> actual = trainingTypeService.findAll().stream()
                    .map(TrainingType::getName)
                    .collect(Collectors.toSet());

            if (!actual.equals(expected)) {
                return Health.down()
                        .withDetail("expectedTypes", expected.size())
                        .withDetail("availableTypes", actual.size())
                        .build();
            }
            return Health.up()
                    .withDetail("availableTypes", actual.size())
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception).build();
        }
    }
}
