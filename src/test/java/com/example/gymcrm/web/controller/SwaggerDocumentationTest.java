package com.example.gymcrm.web.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
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
    void everyRestControllerMethodHasSwagger2OperationAndResponseDocumentation() {
        List<Method> endpointMethods = CONTROLLERS.stream()
                .flatMap(controller -> List.of(controller.getDeclaredMethods()).stream())
                .filter(this::isEndpoint)
                .toList();

        assertThat(endpointMethods).isNotEmpty();
        assertThat(endpointMethods)
                .allMatch(method -> method.isAnnotationPresent(ApiOperation.class),
                        "every endpoint should have @ApiOperation")
                .allMatch(method -> method.isAnnotationPresent(ApiResponses.class),
                        "every endpoint should have @ApiResponses");
    }

    private boolean isEndpoint(Method method) {
        return List.of(method.getDeclaredAnnotations()).stream()
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class));
    }
}
