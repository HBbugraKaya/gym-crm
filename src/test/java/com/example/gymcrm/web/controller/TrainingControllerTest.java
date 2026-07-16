package com.example.gymcrm.web.controller;

import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.AddTrainingCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.web.dto.AddTrainingRequest;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials("alice.coach", "secret");

    @Mock
    private GymFacade gymFacade;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private TrainingController controller;

    @Test
    void addTrainingDelegatesValidatedRequestWithoutClientSuppliedType() {
        LocalDate date = LocalDate.of(2026, 7, 16);
        var request = new AddTrainingRequest(
                "john.smith", "alice.coach", "Morning yoga", date, 60);
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);

        var response = controller.addTraining(AUTHORIZATION, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(gymFacade).addTraining(CREDENTIALS, new AddTrainingCommand(
                "john.smith", "alice.coach", "Morning yoga", date, 60));
    }
}
