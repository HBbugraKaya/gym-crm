package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.service.TrainingTypeService;
import com.example.gymcrm.service.UserAccountService;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.web.dto.TrainingTypeResponse;
import com.example.gymcrm.web.mapper.GymWebMapper;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials("john.smith", "secret");

    @Mock
    private TrainingTypeService trainingTypeService;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private GymWebMapper mapper;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private TrainingTypeController controller;

    @Test
    void getTrainingTypesAuthenticatesBeforeReadingAndMapsEntities() {
        List<TrainingType> entities = List.of();
        List<TrainingTypeResponse> expected = List.of();
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);
        when(trainingTypeService.findAll()).thenReturn(entities);
        when(mapper.toTrainingTypes(entities)).thenReturn(expected);

        var result = controller.getTrainingTypes(AUTHORIZATION);

        assertThat(result).isSameAs(expected);
        InOrder order = inOrder(credentialsResolver, userAccountService, trainingTypeService, mapper);
        order.verify(credentialsResolver).resolve(AUTHORIZATION);
        order.verify(userAccountService).authenticate(CREDENTIALS);
        order.verify(trainingTypeService).findAll();
        order.verify(mapper).toTrainingTypes(entities);
    }
}
