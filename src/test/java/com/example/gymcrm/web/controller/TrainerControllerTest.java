package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.CreateTrainerCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.UpdateTrainerCommand;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.UpdateTrainerRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
import com.example.gymcrm.web.security.RequestCredentialsResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerControllerTest {
    private static final String USERNAME = "alice.coach";
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials(USERNAME, "secret");

    @Mock
    private GymFacade gymFacade;
    @Mock
    private GymWebMapper mapper;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private TrainerController controller;

    @Test
    void registerCreatesActiveTrainerAndReturnsGeneratedCredentials() {
        var request = new TrainerRegistrationRequest("Alice", "Coach", TrainingTypeName.YOGA);
        var command = new CreateTrainerCommand("Alice", "Coach", TrainingTypeName.YOGA, true);
        Trainer trainer = mock(Trainer.class);
        var expected = new RegistrationResponse(USERNAME, "generated-password");
        when(gymFacade.createTrainer(command)).thenReturn(trainer);
        when(mapper.toRegistrationResponse(trainer)).thenReturn(expected);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(gymFacade).createTrainer(command);
        verify(mapper).toRegistrationResponse(trainer);
    }

    @Test
    void getProfileResolvesCredentialsAndMapsEntity() {
        Trainer trainer = mock(Trainer.class);
        var expected = new TrainerProfileResponse(
                USERNAME, "Alice", "Coach", TrainingTypeName.YOGA, true, List.of());
        authenticate();
        when(gymFacade.getTrainerProfile(CREDENTIALS, USERNAME)).thenReturn(trainer);
        when(mapper.toTrainerProfile(trainer)).thenReturn(expected);

        var result = controller.getProfile(USERNAME, AUTHORIZATION);

        assertThat(result).isSameAs(expected);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(gymFacade).getTrainerProfile(CREDENTIALS, USERNAME);
    }

    @Test
    void updateProfileDelegatesPathAndEditableFields() {
        Trainer updated = mock(Trainer.class);
        var request = new UpdateTrainerRequest("Alicia", "Coach", false);
        var command = new UpdateTrainerCommand("Alicia", "Coach", false);
        var expected = new TrainerProfileResponse(
                USERNAME, "Alicia", "Coach", TrainingTypeName.YOGA, false, List.of());
        authenticate();
        when(gymFacade.updateTrainer(CREDENTIALS, USERNAME, command)).thenReturn(updated);
        when(mapper.toTrainerProfile(updated)).thenReturn(expected);

        var result = controller.updateProfile(USERNAME, AUTHORIZATION, request);

        assertThat(result).isSameAs(expected);
        verify(gymFacade).updateTrainer(CREDENTIALS, USERNAME, command);
    }

    @Test
    void getTrainingsBuildsCriteriaAndMapsFilteredResult() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        var criteria = new TrainerTrainingCriteria(from, to, "John");
        List<Training> trainings = List.of(mock(Training.class));
        List<TrainerTrainingResponse> expected = List.of();
        authenticate();
        when(gymFacade.getTrainerTrainings(CREDENTIALS, USERNAME, criteria)).thenReturn(trainings);
        when(mapper.toTrainerTrainings(trainings)).thenReturn(expected);

        var result = controller.getTrainings(USERNAME, AUTHORIZATION, from, to, "John");

        assertThat(result).isSameAs(expected);
        verify(gymFacade).getTrainerTrainings(CREDENTIALS, USERNAME, criteria);
        verify(mapper).toTrainerTrainings(trainings);
    }

    private void authenticate() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);
    }
}
