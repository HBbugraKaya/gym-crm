package com.example.gymcrm.service;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeServiceTest {
    @Mock
    private TrainingTypeRepository trainingTypeRepository;

    @InjectMocks
    private TrainingTypeService service;

    @Test
    void findAllReturnsRepositoryTrainingTypesInOrder() {
        TrainingType yoga = new TrainingType(TrainingTypeName.YOGA);
        TrainingType cardio = new TrainingType(TrainingTypeName.CARDIO);
        when(trainingTypeRepository.findAll()).thenReturn(List.of(yoga, cardio));

        assertThat(service.findAll()).containsExactly(yoga, cardio);
    }
}
