package com.example.gymcrm.health;

import com.example.gymcrm.repository.TrainingTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TrainingTypeHealthIndicator implements HealthIndicator {

    private final TrainingTypeRepository trainingTypeRepository;

    @Override
    public Health health() {
        long count = trainingTypeRepository.count();
        if (count > 0) {
            return Health.up()
                    .withDetail("trainingTypeCount", count)
                    .build();
        }
        return Health.down()
                .withDetail("trainingTypeCount", count)
                .build();
    }
}
