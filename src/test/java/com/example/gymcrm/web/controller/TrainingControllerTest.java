package com.example.gymcrm.web.controller;

import com.example.gymcrm.service.TrainingService;
import com.example.gymcrm.web.dto.AddTrainingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {
    @Mock
    private TrainingService trainingService;
    @InjectMocks
    private TrainingController controller;

    @Test
    void addTrainingDelegatesValidatedRequestWithoutClientSuppliedType() {
        LocalDate date = LocalDate.of(2026, 7, 16);
        var request = new AddTrainingRequest(
                "john.smith", "alice.coach", "Morning yoga", date, 60);

        var response = controller.addTraining(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(trainingService).addTraining(
                "john.smith", "alice.coach", "Morning yoga", date, 60);
    }

    @Test
    void deleteTrainingDelegatesTrainingId() {
        var response = controller.deleteTraining(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(trainingService).deleteTraining(42L);
    }
}
