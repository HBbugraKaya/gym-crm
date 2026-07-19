package com.example.gymcrm.service;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingTypeService {
    private final TrainingTypeRepository trainingTypeRepository;

    public TrainingTypeService(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainingType> findAll() {
        return List.copyOf(trainingTypeRepository.findAll());
    }
}
