package com.example.gymcrm.report.web;

import com.example.gymcrm.report.service.TraineeReportService;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportEntry;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trainee-deletion-reports")
@Tag(name = "Trainee Reports", description = "Trainee deletion report events")
@SecurityRequirement(name = "bearerAuth")
public class TraineeReportController {
    private final TraineeReportService traineeReportService;

    public TraineeReportController(TraineeReportService traineeReportService) {
        this.traineeReportService = traineeReportService;
    }

    @PostMapping
    @Operation(summary = "Record a trainee deletion")
    public ResponseEntity<Void> recordDeletion(@Valid @RequestBody TraineeDeletionReportRequest request) {
        traineeReportService.recordDeletion(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get recorded trainee deletion reports")
    public List<TraineeDeletionReportEntry> findAll() {
        return traineeReportService.findAll();
    }
}
