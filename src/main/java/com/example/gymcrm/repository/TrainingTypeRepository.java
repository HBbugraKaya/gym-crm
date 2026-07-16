package com.example.gymcrm.repository;

import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public TrainingType save(TrainingType trainingType) {
        entityManager.persist(trainingType);
        return trainingType;
    }

    public Optional<TrainingType> findByName(TrainingTypeName name) {
        return entityManager.createQuery("""
                        select tt
                        from TrainingType tt
                        where tt.name = :name
                        """, TrainingType.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    public List<TrainingType> findAll() {
        return entityManager.createQuery("""
                        select tt
                        from TrainingType tt
                        order by tt.id
                        """, TrainingType.class)
                .getResultList();
    }

    public boolean existsByName(TrainingTypeName name) {
        return entityManager.createQuery("""
                        select count(tt)
                        from TrainingType tt
                        where tt.name = :name
                        """, Long.class)
                .setParameter("name", name)
                .getSingleResult() > 0;
    }
}
