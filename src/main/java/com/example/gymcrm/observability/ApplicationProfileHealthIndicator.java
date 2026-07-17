package com.example.gymcrm.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component("applicationProfileHealthIndicator")
public class ApplicationProfileHealthIndicator implements HealthIndicator {
    private static final Set<String> SUPPORTED_PROFILES = Set.of("local", "dev", "stg", "prod");

    private final Environment environment;

    public ApplicationProfileHealthIndicator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Health health() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }

        long environmentProfileCount = Arrays.stream(profiles)
                .filter(SUPPORTED_PROFILES::contains)
                .count();
        Health.Builder health = environmentProfileCount == 1 ? Health.up() : Health.down();
        return health.withDetail("profiles", profiles).build();
    }
}
