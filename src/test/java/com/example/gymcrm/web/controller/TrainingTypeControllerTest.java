package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.repository.TrainingTypeRepository;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {
    @Mock
    private TrainingTypeRepository trainingTypeRepository;
    @Mock
    private GymWebMapper mapper;
    @InjectMocks
    private TrainingTypeController controller;

    @Test
    void getTrainingTypesReadsCatalogAndMapsEntities() {
        List<TrainingType> entities = List.of();
        List<TrainingTypeResponse> expected = List.of();
        when(trainingTypeRepository.findAll()).thenReturn(entities);
        when(mapper.toTrainingTypes(entities)).thenReturn(expected);

        var result = controller.getTrainingTypes();

        assertThat(result).isSameAs(expected);
        verify(trainingTypeRepository).findAll();
        verify(mapper).toTrainingTypes(entities);
    }
}
