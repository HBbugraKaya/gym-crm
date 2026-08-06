package com.example.gymcrm.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.gymcrm.entity.TrainingType;
import com.example.gymcrm.entity.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;

@Component
@Order(1)
public class TrainingTypeSeed implements ApplicationRunner {

    private final TrainingTypeRepository trainingTypeRepository;

    public TrainingTypeSeed(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (TrainingTypeName type : TrainingTypeName.values()) {
            if (!trainingTypeRepository.existsByName(type)) {
                TrainingType trainingType = new TrainingType();
                trainingType.setName(type);
                trainingTypeRepository.save(trainingType);
            }
        }
    }
}
