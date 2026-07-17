package com.example.gymcrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest {

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
