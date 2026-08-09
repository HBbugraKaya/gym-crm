package com.example.gymcrm.workload.web;

import com.example.gymcrm.workload.service.TrainerWorkloadService;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainerWorkloadControllerTest {
    private final TrainerWorkloadService service = mock(TrainerWorkloadService.class);
    private final TrainerWorkloadController controller = new TrainerWorkloadController(service);

    @Test
    void applyReturnsOkAndDelegatesRequest() {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                "Coach.One",
                "Coach",
                "One",
                true,
                LocalDate.of(2026, 8, 5),
                45,
                TrainerWorkloadRequest.WorkloadAction.ADD);

        assertThat(controller.apply(request).getStatusCode().value()).isEqualTo(200);

        verify(service).apply(request);
    }

    @Test
    void findMonthlyReturnsServiceResponse() {
        MonthlyWorkloadResponse response = new MonthlyWorkloadResponse(
                "Coach.One", "Coach", "One", true, 2026, 8, 45);
        when(service.findMonthly("Coach.One", 2026, 8)).thenReturn(response);

        assertThat(controller.findMonthly("Coach.One", 2026, 8)).isSameAs(response);
    }

    @Test
    void findSummaryReturnsServiceResponse() {
        TrainerWorkloadSummary response = new TrainerWorkloadSummary(
                "Coach.One", "Coach", "One", true, List.of());
        when(service.findSummary("Coach.One")).thenReturn(response);

        assertThat(controller.findSummary("Coach.One")).isSameAs(response);
    }
}
