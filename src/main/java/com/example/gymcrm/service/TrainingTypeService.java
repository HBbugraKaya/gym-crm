package com.example.gymcrm.service;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.repository.TrainingTypeRepository;

@Service
@RequiredArgsConstructor
public class TrainingTypeService {
    private final TrainingTypeRepository trainingTypeRepository;

    public List<TrainingType> getTrainingTypes(){
        return trainingTypeRepository.findAll();
    }
}
