package com.example.gymcrm.web.controller;

import com.example.gymcrm.web.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerDocumentationTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            AuthenticationController.class,
            UserAccountController.class,
            TraineeController.class,
            TrainerController.class,
            TrainingController.class,
            TrainingTypeController.class
    );

    @Test
    void everyRestControllerHasOpenApiTag() {
        assertThat(CONTROLLERS)
                .allMatch(controller -> controller.isAnnotationPresent(Tag.class),
                        "every controller should have an OpenAPI tag");
    }

    @Test
    void everyEndpointHasAnOpenApiSummary() {
        endpointMethods().forEach(method -> {
            Operation operation = method.getAnnotation(Operation.class);
            assertThat(operation)
                    .as("OpenAPI operation for %s", method)
                    .isNotNull();
            assertThat(operation.summary())
                    .as("OpenAPI summary for %s", method)
                    .isNotBlank();
        });
    }

    @Test
    void everyProtectedEndpointReferencesTheBasicAuthScheme() {
        assertThat(endpointMethods())
                .allSatisfy(method -> assertThat(hasBasicAuthRequirement(method))
                        .as("HTTP Basic requirement for %s", method)
                        .isEqualTo(!isPublicRegistration(method)));
    }

    @Test
    void openApiDefinitionPublishesApiInfoAndHttpBasicScheme() {
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertThat(definition.info().title()).isEqualTo("Gym CRM API");
        assertThat(definition.info().version()).isEqualTo("1.0");
        assertThat(scheme.name()).isEqualTo(OpenApiConfig.BASIC_AUTH_SCHEME);
        assertThat(scheme.type()).isEqualTo(SecuritySchemeType.HTTP);
        assertThat(scheme.scheme()).isEqualTo("basic");
    }

    private boolean isEndpoint(Method method) {
        return List.of(method.getDeclaredAnnotations()).stream()
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class));
    }

    private List<Method> endpointMethods() {
        return CONTROLLERS.stream()
                .flatMap(controller -> List.of(controller.getDeclaredMethods()).stream())
                .filter(this::isEndpoint)
                .toList();
    }

    private boolean isPublicRegistration(Method method) {
        return method.getName().equals("register")
                && (method.getDeclaringClass() == TraineeController.class
                || method.getDeclaringClass() == TrainerController.class);
    }

    private boolean hasBasicAuthRequirement(Method method) {
        return hasBasicAuthRequirement(method.getAnnotationsByType(SecurityRequirement.class))
                || hasBasicAuthRequirement(method.getDeclaringClass()
                .getAnnotationsByType(SecurityRequirement.class));
    }

    private boolean hasBasicAuthRequirement(SecurityRequirement[] requirements) {
        return List.of(requirements).stream()
                .anyMatch(requirement -> requirement.name().equals(OpenApiConfig.BASIC_AUTH_SCHEME));
    }
}
