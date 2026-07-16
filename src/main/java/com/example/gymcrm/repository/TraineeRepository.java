package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class TraineeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Trainee save(Trainee trainee) {
        if (trainee.getId() == null) {
            entityManager.persist(trainee);
            return trainee;
        }
        return entityManager.merge(trainee);
    }

    public Optional<Trainee> findByUsername(String username) {
        return entityManager.createQuery("""
                        select t
                        from Trainee t
                        join fetch t.user u
                        where lower(u.username) = :username
                        """, Trainee.class)
                .setParameter("username", normalize(username))
                .getResultStream()
                .findFirst();
    }

    public Optional<Trainee> findByUsernameWithTrainers(String username) {
        return entityManager.createQuery("""
                        select distinct t
                        from Trainee t
                        join fetch t.user u
                        left join fetch t.trainers tr
                        left join fetch tr.user tru
                        left join fetch tr.specialization
                        where lower(u.username) = :username
                        """, Trainee.class)
                .setParameter("username", normalize(username))
                .getResultStream()
                .findFirst();
    }

    public List<Trainee> findAll() {
        return entityManager.createQuery("""
                        select t
                        from Trainee t
                        join fetch t.user
                        order by t.id
                        """, Trainee.class)
                .getResultList();
    }

    public void delete(Trainee trainee) {
        entityManager.remove(entityManager.contains(trainee) ? trainee : entityManager.merge(trainee));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
