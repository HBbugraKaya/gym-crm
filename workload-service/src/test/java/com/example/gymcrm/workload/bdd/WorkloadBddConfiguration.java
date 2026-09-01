package com.example.gymcrm.workload.bdd;

import com.example.gymcrm.workload.domain.TrainerWorkload;
import com.example.gymcrm.workload.repository.TrainerWorkloadRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

@TestConfiguration
class WorkloadBddConfiguration {
    @Bean
    WorkloadDocuments workloadDocuments() {
        return new WorkloadDocuments();
    }

    @Bean
    @Primary
    TrainerWorkloadRepository trainerWorkloadRepository(WorkloadDocuments documents) {
        TrainerWorkloadRepository repository = mock(TrainerWorkloadRepository.class, withSettings().lenient());
        when(repository.findByTrainerUsernameIgnoreCase(any())).thenAnswer(invocation ->
                documents.find(invocation.getArgument(0)));
        when(repository.save(any(TrainerWorkload.class))).thenAnswer(invocation ->
                documents.save(invocation.getArgument(0)));
        return repository;
    }

    static final class WorkloadDocuments {
        private final Map<String, TrainerWorkload> documents = new ConcurrentHashMap<>();

        void clear() {
            documents.clear();
        }

        Optional<TrainerWorkload> find(String trainerUsername) {
            if (trainerUsername == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(documents.get(trainerUsername.toLowerCase(Locale.ROOT)));
        }

        TrainerWorkload save(TrainerWorkload workload) {
            documents.put(workload.getTrainerUsername().toLowerCase(Locale.ROOT), workload);
            return workload;
        }
    }
}
