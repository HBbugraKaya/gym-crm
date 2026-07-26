package com.example.gymcrm.observability;

import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingTypeCatalogHealthIndicatorTest {

    private final TrainingTypeRepository trainingTypeRepository = mock(TrainingTypeRepository.class);
    private final TrainingTypeCatalogHealthIndicator indicator =
            new TrainingTypeCatalogHealthIndicator(trainingTypeRepository);

    @Test
    void reportsUpWhenTheCompleteCatalogIsAvailable() {
        when(trainingTypeRepository.count()).thenReturn((long) TrainingTypeName.values().length);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenTheCatalogIsIncompleteOrUnavailable() {
        when(trainingTypeRepository.count()).thenReturn(1L);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);

        when(trainingTypeRepository.count()).thenThrow(new IllegalStateException("database unavailable"));
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
