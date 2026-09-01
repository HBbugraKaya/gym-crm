package com.example.gymcrm.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.gymcrm.repository.TrainerWorkloadRepository;
import com.example.gymcrm.web.dto.ActionType;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {

    @Mock
    private TrainerWorkloadRepository repository;

    @InjectMocks
    private TrainerWorkloadService service;

    @Test
    void deleteDoesNotCreateMissingTrainer() {
        when(repository.findById("jane.doe")).thenReturn(Optional.empty());

        service.processWorkload(new TrainerWorkloadRequest(
                "jane.doe", "Jane", "Doe", true, LocalDate.of(2026, 8, 1), 60, ActionType.DELETE));

        verify(repository, never()).save(any());
    }
}
