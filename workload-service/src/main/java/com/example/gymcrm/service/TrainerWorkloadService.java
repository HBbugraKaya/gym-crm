package com.example.gymcrm.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.gymcrm.dto.ActionType;
import com.example.gymcrm.dto.TrainerWorkloadRequest;
import com.example.gymcrm.entity.TrainerWorkload;

@Service
public class TrainerWorkloadService {
    private final Map<String, TrainerWorkload> workloadStorage = new ConcurrentHashMap<>();

    public void processWorkload(TrainerWorkloadRequest request) {
        TrainerWorkload workload = workloadStorage.computeIfAbsent(
            request.trainerUsername().toLowerCase(), username -> new TrainerWorkload(
                                                            request.trainerUsername(),
                                                            request.trainerFirstName(),
                                                            request.trainerLastName(),
                                                            request.isActive(),
                                                            new HashMap<>()
                                                            )
        );

        workload.setFirstName(request.trainerFirstName());
        workload.setLastName(request.trainerLastName());
        workload.setActive(request.isActive());

        int year = request.trainingDate().getYear();
        int month = request.trainingDate().getMonthValue();

        Map<Integer, Integer> months = workload.getYears().computeIfAbsent(year, y -> new HashMap<>());
        int currentDuration = months.getOrDefault(month, 0);

        if (request.actionType() == ActionType.ADD) {
            months.put(month, currentDuration + request.trainingDuration());
        } else if (request.actionType() == ActionType.DELETE) {
            int newDuration = Math.max(0, currentDuration - request.trainingDuration());
            months.put(month, newDuration);
        }
    }
}
