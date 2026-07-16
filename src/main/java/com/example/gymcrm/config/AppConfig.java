package com.example.gymcrm.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
        "com.example.gymcrm.facade",
        "com.example.gymcrm.generator",
        "com.example.gymcrm.repository",
        "com.example.gymcrm.service"
})
@Import({PersistenceConfig.class, TrainingTypeInitializer.class})
public class AppConfig {
}
