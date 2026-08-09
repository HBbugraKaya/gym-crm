package com.example.gymcrm.report.web;

import com.example.gymcrm.report.service.TraineeReportService;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TraineeReportControllerTest {
    private final TraineeReportService service = mock(TraineeReportService.class);
    private final TraineeReportController controller = new TraineeReportController(service);

    @Test
    void recordDeletionReturnsOkAndDelegates() {
        TraineeDeletionReportRequest request = new TraineeDeletionReportRequest(
                "runner.one", "Runner", "One", true);

        assertThat(controller.recordDeletion(request).getStatusCode().value()).isEqualTo(200);

        verify(service).recordDeletion(request);
    }

    @Test
    void findAllDelegatesToService() {
        controller.findAll();

        verify(service).findAll();
    }
}
