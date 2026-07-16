package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.exception.AuthenticationException;
import com.example.gymcrm.facade.GymFacade;
import com.example.gymcrm.service.command.CreateTraineeCommand;
import com.example.gymcrm.service.command.Credentials;
import com.example.gymcrm.service.command.UpdateTraineeCommand;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TraineeProfileResponse;
import com.example.gymcrm.web.dto.TraineeRegistrationRequest;
import com.example.gymcrm.web.dto.TraineeTrainingResponse;
import com.example.gymcrm.web.dto.TrainerAssignmentsRequest;
import com.example.gymcrm.web.dto.TrainerSummaryResponse;
import com.example.gymcrm.web.dto.UpdateTraineeRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeControllerTest {
    private static final String USERNAME = "john.smith";
    private static final String AUTHORIZATION = "Basic encoded";
    private static final Credentials CREDENTIALS = new Credentials(USERNAME, "secret");

    @Mock
    private GymFacade gymFacade;
    @Mock
    private GymWebMapper mapper;
    @Mock
    private RequestCredentialsResolver credentialsResolver;
    @InjectMocks
    private TraineeController controller;

    @Test
    void registerCreatesActiveTraineeAndReturnsGeneratedCredentials() {
        LocalDate birthDate = LocalDate.of(2001, 1, 1);
        var request = new TraineeRegistrationRequest("John", "Smith", birthDate, "Istanbul");
        var command = new CreateTraineeCommand("John", "Smith", birthDate, "Istanbul", true);
        Trainee trainee = mock(Trainee.class);
        var expected = new RegistrationResponse(USERNAME, "generated-password");
        when(gymFacade.createTrainee(command)).thenReturn(trainee);
        when(mapper.toRegistrationResponse(trainee)).thenReturn(expected);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(gymFacade).createTrainee(command);
        verify(mapper).toRegistrationResponse(trainee);
    }

    @Test
    void getProfileResolvesCredentialsAndMapsEntity() {
        Trainee trainee = mock(Trainee.class);
        var expected = new TraineeProfileResponse(
                USERNAME, "John", "Smith", null, null, true, List.of());
        authenticate();
        when(gymFacade.getTraineeProfile(CREDENTIALS, USERNAME)).thenReturn(trainee);
        when(mapper.toTraineeProfile(trainee)).thenReturn(expected);

        var result = controller.getProfile(USERNAME, AUTHORIZATION);

        assertThat(result).isSameAs(expected);
        verify(credentialsResolver).resolve(AUTHORIZATION);
        verify(gymFacade).getTraineeProfile(CREDENTIALS, USERNAME);
    }

    @Test
    void updateProfileDelegatesPathAndEditableFields() {
        LocalDate birthDate = LocalDate.of(2000, 2, 2);
        var request = new UpdateTraineeRequest("Johnny", "Smith", birthDate, "Ankara", false);
        var command = new UpdateTraineeCommand("Johnny", "Smith", birthDate, "Ankara", false);
        Trainee updated = mock(Trainee.class);
        var expected = new TraineeProfileResponse(
                USERNAME, "Johnny", "Smith", birthDate, "Ankara", false, List.of());
        authenticate();
        when(gymFacade.updateTrainee(CREDENTIALS, USERNAME, command)).thenReturn(updated);
        when(mapper.toTraineeProfile(updated)).thenReturn(expected);

        var result = controller.updateProfile(USERNAME, AUTHORIZATION, request);

        assertThat(result).isSameAs(expected);
        verify(gymFacade).updateTrainee(CREDENTIALS, USERNAME, command);
    }

    @Test
    void deleteProfileDelegatesHardDeleteAndReturnsOk() {
        authenticate();

        var response = controller.deleteProfile(USERNAME, AUTHORIZATION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gymFacade).deleteTrainee(CREDENTIALS, USERNAME);
    }

    @Test
    void getAvailableTrainersMapsFacadeResult() {
        List<Trainer> trainers = List.of(mock(Trainer.class));
        List<TrainerSummaryResponse> expected = List.of(
                new TrainerSummaryResponse("alice.coach", "Alice", "Coach", TrainingTypeName.YOGA));
        authenticate();
        when(gymFacade.getUnassignedTrainers(CREDENTIALS, USERNAME)).thenReturn(trainers);
        when(mapper.toTrainerSummaries(trainers)).thenReturn(expected);

        var result = controller.getAvailableTrainers(USERNAME, AUTHORIZATION);

        assertThat(result).isSameAs(expected);
        verify(gymFacade).getUnassignedTrainers(CREDENTIALS, USERNAME);
        verify(mapper).toTrainerSummaries(trainers);
    }

    @Test
    void updateTrainersDelegatesImmutableUsernameListAndMapsResult() {
        var request = new TrainerAssignmentsRequest(List.of("alice.coach", "bob.coach"));
        List<Trainer> trainers = List.of(mock(Trainer.class));
        List<TrainerSummaryResponse> expected = List.of();
        authenticate();
        when(gymFacade.updateTraineeTrainers(CREDENTIALS, USERNAME, request.trainerUsernames()))
                .thenReturn(trainers);
        when(mapper.toTrainerSummaries(trainers)).thenReturn(expected);

        var result = controller.updateTrainers(USERNAME, AUTHORIZATION, request);

        assertThat(result).isSameAs(expected);
        verify(gymFacade).updateTraineeTrainers(CREDENTIALS, USERNAME, request.trainerUsernames());
        verify(mapper).toTrainerSummaries(trainers);
    }

    @Test
    void getTrainingsBuildsCriteriaAndMapsFilteredResult() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        var criteria = new TraineeTrainingCriteria(from, to, "Alice", TrainingTypeName.YOGA);
        List<Training> trainings = List.of(mock(Training.class));
        List<TraineeTrainingResponse> expected = List.of();
        authenticate();
        when(gymFacade.getTraineeTrainings(CREDENTIALS, USERNAME, criteria)).thenReturn(trainings);
        when(mapper.toTraineeTrainings(trainings)).thenReturn(expected);

        var result = controller.getTrainings(
                USERNAME, AUTHORIZATION, from, to, "Alice", TrainingTypeName.YOGA);

        assertThat(result).isSameAs(expected);
        verify(gymFacade).getTraineeTrainings(CREDENTIALS, USERNAME, criteria);
        verify(mapper).toTraineeTrainings(trainings);
    }

    @Test
    void protectedEndpointStopsWhenAuthorizationCannotBeResolved() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenThrow(new AuthenticationException("Basic"));

        assertThatThrownBy(() -> controller.getProfile(USERNAME, AUTHORIZATION))
                .isInstanceOf(AuthenticationException.class);
        verifyNoInteractions(gymFacade, mapper);
    }

    private void authenticate() {
        when(credentialsResolver.resolve(AUTHORIZATION)).thenReturn(CREDENTIALS);
    }
}
