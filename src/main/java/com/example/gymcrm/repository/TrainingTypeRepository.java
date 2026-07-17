package com.example.gymcrm.repository;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainingTypeRepository extends JpaRepository<TrainingType, Long> {

    Optional<TrainingType> findByName(TrainingTypeName name);

    boolean existsByName(TrainingTypeName name);

    @Override
    @Query("""
            select tt
            from TrainingType tt
            order by tt.id
            """)
    List<TrainingType> findAll();
}
