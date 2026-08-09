package com.example.gymcrm.report.service;

import com.example.gymcrm.report.web.dto.TraineeDeletionReportEntry;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TraineeReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraineeReportService.class);

    private final Clock clock;
    private final List<TraineeDeletionReportEntry> deletionReports = new CopyOnWriteArrayList<>();

    public TraineeReportService(Clock clock) {
        this.clock = clock;
    }

    public void recordDeletion(TraineeDeletionReportRequest request) {
        deletionReports.add(new TraineeDeletionReportEntry(
                request.traineeUsername(),
                request.traineeFirstName(),
                request.traineeLastName(),
                request.active(),
                clock.instant()));
        LOGGER.info(
                "Trainee deletion report recorded traineeUsername={} reportCount={}",
                request.traineeUsername(),
                deletionReports.size());
    }

    public List<TraineeDeletionReportEntry> findAll() {
        return List.copyOf(deletionReports);
    }
}
