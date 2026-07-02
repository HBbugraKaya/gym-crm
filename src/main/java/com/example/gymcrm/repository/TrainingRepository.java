package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.service.criteria.TraineeTrainingCriteria;
import com.example.gymcrm.service.criteria.TrainerTrainingCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class TrainingRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Training save(Training training) {
        entityManager.persist(training);
        return training;
    }

    public List<Training> findAll() {
        return entityManager.createQuery("""
                        select trn
                        from Training trn
                        join fetch trn.trainee t
                        join fetch t.user
                        join fetch trn.trainer tr
                        join fetch tr.user
                        join fetch trn.trainingType
                        order by trn.id
                        """, Training.class)
                .getResultList();
    }

    public List<Training> findByTraineeUsername(String traineeUsername, TraineeTrainingCriteria criteria) {
        TraineeTrainingCriteria effectiveCriteria = criteria == null ? TraineeTrainingCriteria.empty() : criteria;
        return entityManager.createQuery("""
                        select trn
                        from Training trn
                        join fetch trn.trainee t
                        join fetch t.user tu
                        join fetch trn.trainer tr
                        join fetch tr.user tru
                        join fetch trn.trainingType tt
                        where lower(tu.username) = :traineeUsername
                          and (:fromDate is null or trn.date >= :fromDate)
                          and (:toDate is null or trn.date <= :toDate)
                          and (:trainingType is null or tt.name = :trainingType)
                          and (:trainerName is null
                               or lower(tru.firstName) like :trainerName
                               or lower(tru.lastName) like :trainerName
                               or lower(concat(concat(tru.firstName, ' '), tru.lastName)) like :trainerName)
                        order by trn.date, trn.id
                        """, Training.class)
                .setParameter("traineeUsername", normalize(traineeUsername))
                .setParameter("fromDate", effectiveCriteria.fromDate())
                .setParameter("toDate", effectiveCriteria.toDate())
                .setParameter("trainingType", effectiveCriteria.trainingType())
                .setParameter("trainerName", likePattern(effectiveCriteria.trainerName()))
                .getResultList();
    }

    public List<Training> findByTrainerUsername(String trainerUsername, TrainerTrainingCriteria criteria) {
        TrainerTrainingCriteria effectiveCriteria = criteria == null ? TrainerTrainingCriteria.empty() : criteria;
        return entityManager.createQuery("""
                        select trn
                        from Training trn
                        join fetch trn.trainee t
                        join fetch t.user tu
                        join fetch trn.trainer tr
                        join fetch tr.user tru
                        join fetch trn.trainingType tt
                        where lower(tru.username) = :trainerUsername
                          and (:fromDate is null or trn.date >= :fromDate)
                          and (:toDate is null or trn.date <= :toDate)
                          and (:traineeName is null
                               or lower(tu.firstName) like :traineeName
                               or lower(tu.lastName) like :traineeName
                               or lower(concat(concat(tu.firstName, ' '), tu.lastName)) like :traineeName)
                        order by trn.date, trn.id
                        """, Training.class)
                .setParameter("trainerUsername", normalize(trainerUsername))
                .setParameter("fromDate", effectiveCriteria.fromDate())
                .setParameter("toDate", effectiveCriteria.toDate())
                .setParameter("traineeName", likePattern(effectiveCriteria.traineeName()))
                .getResultList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + normalize(value) + "%";
    }
}
