package com.example.gymcrm.config;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.repository.TrainingTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrainingTypeInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingTypeInitializer.class);

    private final TrainingTypeRepository trainingTypeRepository;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public TrainingTypeInitializer(TrainingTypeRepository trainingTypeRepository,
                                   TransactionTemplate transactionTemplate) {
        this.trainingTypeRepository = trainingTypeRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

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
