package com.example.gymcrm.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(ScenarioState.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.discovery.enabled=false",
        "spring.activemq.in-memory=true",
        "spring.activemq.broker-url=vm://cucumber-gym-crm?broker.persistent=false"
})
public class CucumberSpringConfiguration {
}
