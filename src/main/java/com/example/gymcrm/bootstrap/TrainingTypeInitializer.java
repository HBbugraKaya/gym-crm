package com.example.gymcrm.bootstrap;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingTypeInitializer implements ApplicationRunner {
    private final TrainingTypeRepository trainingTypeRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (TrainingTypeName name : TrainingTypeName.values()) {
            if (!trainingTypeRepository.existsByName(name)) {
                trainingTypeRepository.save(new TrainingType(name));
            }
        }
    }
}
