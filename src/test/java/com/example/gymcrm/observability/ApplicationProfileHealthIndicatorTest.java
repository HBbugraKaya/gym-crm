package com.example.gymcrm.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationProfileHealthIndicatorTest {

    @Test
    void reportsUpForOneEnvironmentPlusAdditionalProfiles() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "cloud");

        assertThat(new ApplicationProfileHealthIndicator(environment).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownForUnsupportedProfile() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("unexpected");

        assertThat(new ApplicationProfileHealthIndicator(environment).health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void reportsDownWhenMultipleEnvironmentProfilesAreActive() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "prod");

        assertThat(new ApplicationProfileHealthIndicator(environment).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
