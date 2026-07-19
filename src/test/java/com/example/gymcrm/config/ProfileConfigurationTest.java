package com.example.gymcrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest {

    @Test
    void baseConfigurationPublishesApplicationInfoAndDefinesTransactionAwareConsoleLogging() {
        Properties properties = properties("application.yml");

        assertThat(properties.getProperty("management.info.env.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("info.application.name")).isEqualTo("${spring.application.name}");
        assertThat(properties.getProperty("info.application.description")).isNotBlank();
        assertThat(properties.getProperty("logging.pattern.console"))
                .contains("transactionId=%X{transactionId:-none}");
        assertThat(properties.getProperty("logging.level.com.example.gymcrm")).isEqualTo("INFO");
        assertThat(properties.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("WARN");
        assertThat(properties.getProperty("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("OFF");
    }

    @Test
    void eachEnvironmentDefinesDistinctDatabaseProperties() {
        List<Properties> profiles = List.of(
                properties("application-local.yml"),
                properties("application-dev.yml"),
                properties("application-stg.yml"),
                properties("application-prod.yml"));

        assertThat(profiles)
                .allSatisfy(properties -> {
                    assertThat(properties.getProperty("spring.datasource.url")).isNotBlank();
                    assertThat(properties.getProperty("spring.datasource.username")).isNotBlank();
                    assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isNotBlank();
                });
        assertThat(profiles.stream()
                .map(properties -> properties.getProperty("spring.datasource.url"))
                .toList())
                .doesNotHaveDuplicates();
    }

    @Test
    void onlyDevelopmentProfilesOverrideTheDefaultApplicationLogLevel() {
        List<Properties> developmentProfiles = List.of(
                properties("application-local.yml"),
                properties("application-dev.yml"));
        List<Properties> deploymentProfiles = List.of(
                properties("application-stg.yml"),
                properties("application-prod.yml"));

        assertThat(developmentProfiles)
                .allSatisfy(properties -> assertThat(properties.getProperty("logging.level.com.example.gymcrm"))
                        .isEqualTo("DEBUG"));
        assertThat(deploymentProfiles)
                .allSatisfy(properties -> assertThat(properties.getProperty("logging.level.com.example.gymcrm"))
                        .isNull());
    }

    private Properties properties(String resourceName) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        Properties properties = factory.getObject();
        if (properties == null) {
            throw new IllegalStateException("Could not load " + resourceName);
        }
        return properties;
    }
}
