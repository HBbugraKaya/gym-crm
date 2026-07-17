package com.example.gymcrm.observability;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.TrainingTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingTypeCatalogHealthIndicatorTest {

    private final TrainingTypeService trainingTypeService = mock(TrainingTypeService.class);
    private final TrainingTypeCatalogHealthIndicator indicator =
            new TrainingTypeCatalogHealthIndicator(trainingTypeService);

    @Test
    void reportsUpWhenTheCompleteCatalogIsAvailable() {
        when(trainingTypeService.findAll()).thenReturn(Arrays.stream(TrainingTypeName.values())
                .map(TrainingType::new)
                .toList());

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenTheCatalogIsIncompleteOrUnavailable() {
        when(trainingTypeService.findAll()).thenReturn(java.util.List.of(new TrainingType(TrainingTypeName.YOGA)));
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);

        when(trainingTypeService.findAll()).thenThrow(new IllegalStateException("database unavailable"));
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
