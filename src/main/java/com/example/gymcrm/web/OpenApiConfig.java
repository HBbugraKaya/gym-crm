package com.example.gymcrm.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Gym CRM API",
        version = "1.0",
        description = "REST API for managing gym trainees, trainers, assignments and training sessions"
))
@SecurityScheme(
        name = OpenApiConfig.BASIC_AUTH_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
    public static final String BASIC_AUTH_SCHEME = "basicAuth";
}
