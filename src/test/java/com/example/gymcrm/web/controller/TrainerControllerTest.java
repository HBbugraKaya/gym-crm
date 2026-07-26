package com.example.gymcrm.web.controller;

import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.TrainerService;
import com.example.gymcrm.service.CreatedAccount;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerProfileResponse;
import com.example.gymcrm.web.dto.TrainerRegistrationRequest;
import com.example.gymcrm.web.dto.TrainerTrainingResponse;
import com.example.gymcrm.web.dto.UpdateTrainerRequest;
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
class TrainerControllerTest {
    private static final String USERNAME = "alice.coach";

    @Mock
    private TrainerService trainerService;
    @Mock
    private GymWebMapper mapper;
    @InjectMocks
    private TrainerController controller;

    @Test
    void registerCreatesActiveTrainerAndReturnsGeneratedCredentials() {
        var request = new TrainerRegistrationRequest("Alice", "Coach", TrainingTypeName.YOGA);
        Trainer trainer = mock(Trainer.class);
        var created = new CreatedAccount<>(trainer, "generated-password");
        var expected = new RegistrationResponse(USERNAME, "generated-password");
        when(trainerService.create("Alice", "Coach", TrainingTypeName.YOGA)).thenReturn(created);
        when(mapper.toRegistrationResponse(created)).thenReturn(expected);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(trainerService).create("Alice", "Coach", TrainingTypeName.YOGA);
        verify(mapper).toRegistrationResponse(created);
    }

    @Test
    void getProfileMapsEntity() {
        Trainer trainer = mock(Trainer.class);
        var expected = new TrainerProfileResponse(
                USERNAME, "Alice", "Coach", TrainingTypeName.YOGA, true, List.of());
        when(trainerService.findByUsername(USERNAME)).thenReturn(trainer);
        when(mapper.toTrainerProfile(trainer)).thenReturn(expected);

        var result = controller.getProfile(USERNAME);

        assertThat(result).isSameAs(expected);
        verify(trainerService).findByUsername(USERNAME);
    }

    @Test
    void updateProfileDelegatesPathAndEditableFields() {
        Trainer updated = mock(Trainer.class);
        var request = new UpdateTrainerRequest("Alicia", "Coach", false);
        var expected = new TrainerProfileResponse(
                USERNAME, "Alicia", "Coach", TrainingTypeName.YOGA, false, List.of());
        when(trainerService.update(USERNAME, "Alicia", "Coach", false)).thenReturn(updated);
        when(mapper.toTrainerProfile(updated)).thenReturn(expected);

        var result = controller.updateProfile(USERNAME, request);

        assertThat(result).isSameAs(expected);
        verify(trainerService).update(USERNAME, "Alicia", "Coach", false);
    }

    @Test
    void getTrainingsBuildsCriteriaAndMapsFilteredResult() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<Training> trainings = List.of(mock(Training.class));
        List<TrainerTrainingResponse> expected = List.of();
        when(trainerService.getTrainings(USERNAME, from, to, "John")).thenReturn(trainings);
        when(mapper.toTrainerTrainings(trainings)).thenReturn(expected);

        var result = controller.getTrainings(USERNAME, from, to, "John");

        assertThat(result).isSameAs(expected);
        verify(trainerService).getTrainings(USERNAME, from, to, "John");
        verify(mapper).toTrainerTrainings(trainings);
    }
}
