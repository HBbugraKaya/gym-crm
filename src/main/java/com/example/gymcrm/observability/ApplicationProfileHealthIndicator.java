package com.example.gymcrm.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ApplicationProfileHealthIndicator implements HealthIndicator {
    private static final Set<String> SUPPORTED_PROFILES = Set.of("local", "dev", "stg", "prod");

    private final Environment environment;

    @Override
    public Health health() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        Health.Builder health = Arrays.stream(profiles).filter(SUPPORTED_PROFILES::contains).count() == 1
                ? Health.up()
                : Health.down();
        return health.withDetail("profiles", profiles).build();
    }
}
