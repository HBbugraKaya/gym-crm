package com.example.gymcrm.workload.web;

import com.example.gymcrm.workload.service.TrainerWorkloadService;
import com.example.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trainer-workloads")
@Tag(name = "Trainer Workloads", description = "Monthly trainer workload summaries")
@SecurityRequirement(name = "bearerAuth")
public class TrainerWorkloadController {
    private final TrainerWorkloadService trainerWorkloadService;

    public TrainerWorkloadController(TrainerWorkloadService trainerWorkloadService) {
        this.trainerWorkloadService = trainerWorkloadService;
    }

    @PostMapping
    @Operation(summary = "Apply a training workload change")
    public ResponseEntity<Void> apply(@Valid @RequestBody TrainerWorkloadRequest request) {
        trainerWorkloadService.apply(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{trainerUsername}")
    @Operation(summary = "Get a trainer workload for one month")
    public MonthlyWorkloadResponse findMonthly(
            @PathVariable String trainerUsername,
            @RequestParam int year,
            @RequestParam int month) {
        return trainerWorkloadService.findMonthly(trainerUsername, year, month);
    }

    @GetMapping("/{trainerUsername}/summary")
    @Operation(summary = "Get the complete trainer workload summary")
    public TrainerWorkloadSummary findSummary(@PathVariable String trainerUsername) {
        return trainerWorkloadService.findSummary(trainerUsername);
    }

}
