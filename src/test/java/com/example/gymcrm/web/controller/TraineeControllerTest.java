package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.TraineeService;
import com.example.gymcrm.service.CreatedAccount;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TraineeProfileResponse;
import com.example.gymcrm.web.dto.TraineeRegistrationRequest;
import com.example.gymcrm.web.dto.TraineeTrainingResponse;
import com.example.gymcrm.web.dto.TrainerAssignmentsRequest;
import com.example.gymcrm.web.dto.TrainerSummaryResponse;
import com.example.gymcrm.web.dto.UpdateTraineeRequest;
import com.example.gymcrm.web.mapper.GymWebMapper;
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
class TraineeControllerTest {
    private static final String USERNAME = "john.smith";

    @Mock
    private TraineeService traineeService;
    @Mock
    private GymWebMapper mapper;
    @InjectMocks
    private TraineeController controller;

    @Test
    void registerCreatesActiveTraineeAndReturnsGeneratedCredentials() {
        LocalDate birthDate = LocalDate.of(2001, 1, 1);
        var request = new TraineeRegistrationRequest("John", "Smith", birthDate, "Istanbul");
        Trainee trainee = mock(Trainee.class);
        var created = new CreatedAccount<>(trainee, "generated-password");
        var expected = new RegistrationResponse(USERNAME, "generated-password");
        when(traineeService.create("John", "Smith", birthDate, "Istanbul")).thenReturn(created);
        when(mapper.toRegistrationResponse(created)).thenReturn(expected);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(traineeService).create("John", "Smith", birthDate, "Istanbul");
        verify(mapper).toRegistrationResponse(created);
    }

    @Test
    void getProfileMapsEntity() {
        Trainee trainee = mock(Trainee.class);
        var expected = new TraineeProfileResponse(
                USERNAME, "John", "Smith", null, null, true, List.of());
        when(traineeService.findByUsername(USERNAME)).thenReturn(trainee);
        when(mapper.toTraineeProfile(trainee)).thenReturn(expected);

        var result = controller.getProfile(USERNAME);

        assertThat(result).isSameAs(expected);
        verify(traineeService).findByUsername(USERNAME);
    }

    @Test
    void updateProfileDelegatesPathAndEditableFields() {
        LocalDate birthDate = LocalDate.of(2000, 2, 2);
        var request = new UpdateTraineeRequest("Johnny", "Smith", birthDate, "Ankara", false);
        Trainee updated = mock(Trainee.class);
        var expected = new TraineeProfileResponse(
                USERNAME, "Johnny", "Smith", birthDate, "Ankara", false, List.of());
        when(traineeService.update(USERNAME, "Johnny", "Smith", birthDate, "Ankara", false))
                .thenReturn(updated);
        when(mapper.toTraineeProfile(updated)).thenReturn(expected);

        var result = controller.updateProfile(USERNAME, request);

        assertThat(result).isSameAs(expected);
        verify(traineeService).update(USERNAME, "Johnny", "Smith", birthDate, "Ankara", false);
    }

    @Test
    void deleteProfileDelegatesHardDeleteAndReturnsOk() {
        var response = controller.deleteProfile(USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(traineeService).deleteByUsername(USERNAME);
    }

    @Test
    void getAvailableTrainersMapsFacadeResult() {
        List<Trainer> trainers = List.of(mock(Trainer.class));
        List<TrainerSummaryResponse> expected = List.of(
                new TrainerSummaryResponse("alice.coach", "Alice", "Coach", TrainingTypeName.YOGA));
        when(traineeService.getUnassignedTrainers(USERNAME)).thenReturn(trainers);
        when(mapper.toTrainerSummaries(trainers)).thenReturn(expected);

        var result = controller.getAvailableTrainers(USERNAME);

        assertThat(result).isSameAs(expected);
        verify(traineeService).getUnassignedTrainers(USERNAME);
        verify(mapper).toTrainerSummaries(trainers);
    }

    @Test
    void updateTrainersDelegatesImmutableUsernameListAndMapsResult() {
        var request = new TrainerAssignmentsRequest(List.of("alice.coach", "bob.coach"));
        List<Trainer> trainers = List.of(mock(Trainer.class));
        List<TrainerSummaryResponse> expected = List.of();
        when(traineeService.updateTrainers(USERNAME, request.trainerUsernames())).thenReturn(trainers);
        when(mapper.toTrainerSummaries(trainers)).thenReturn(expected);

        var result = controller.updateTrainers(USERNAME, request);

        assertThat(result).isSameAs(expected);
        verify(traineeService).updateTrainers(USERNAME, request.trainerUsernames());
        verify(mapper).toTrainerSummaries(trainers);
    }

    @Test
    void getTrainingsBuildsCriteriaAndMapsFilteredResult() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<Training> trainings = List.of(mock(Training.class));
        List<TraineeTrainingResponse> expected = List.of();
        when(traineeService.getTrainings(USERNAME, from, to, "Alice", TrainingTypeName.YOGA))
                .thenReturn(trainings);
        when(mapper.toTraineeTrainings(trainings)).thenReturn(expected);

        var result = controller.getTrainings(USERNAME, from, to, "Alice", TrainingTypeName.YOGA);

        assertThat(result).isSameAs(expected);
        verify(traineeService).getTrainings(USERNAME, from, to, "Alice", TrainingTypeName.YOGA);
        verify(mapper).toTraineeTrainings(trainings);
    }
}
