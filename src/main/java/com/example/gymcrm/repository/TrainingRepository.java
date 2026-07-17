package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long>, TrainingRepositoryCustom {

    @Override
    @Query("""
            select trn
            from Training trn
            join fetch trn.trainee t
            join fetch t.user
            join fetch trn.trainer tr
            join fetch tr.user
            join fetch trn.trainingType
            order by trn.id
            """)
    List<Training> findAll();
}
