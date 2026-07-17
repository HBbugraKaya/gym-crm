package com.example.gymcrm.config;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TrainingTypeInitializer implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingTypeInitializer.class);

    private final TrainingTypeRepository trainingTypeRepository;
    private final TransactionTemplate transactionTemplate;

    public TrainingTypeInitializer(TrainingTypeRepository trainingTypeRepository,
                                   TransactionTemplate transactionTemplate) {
        this.trainingTypeRepository = trainingTypeRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> {
            for (TrainingTypeName name : TrainingTypeName.values()) {
                if (!trainingTypeRepository.existsByName(name)) {
                    trainingTypeRepository.save(new TrainingType(name));
                    LOGGER.debug("Seeded training type name={}", name);
                }
            }
        });
        LOGGER.info("Training types are ready");
    }
}
