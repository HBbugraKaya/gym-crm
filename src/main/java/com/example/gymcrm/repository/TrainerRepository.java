package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class TrainerRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Trainer save(Trainer trainer) {
        if (trainer.getId() == null) {
            entityManager.persist(trainer);
            return trainer;
        }
        return entityManager.merge(trainer);
    }

    public Optional<Trainer> findByUsername(String username) {
        return entityManager.createQuery("""
                        select tr
                        from Trainer tr
                        join fetch tr.user u
                        join fetch tr.specialization
                        where lower(u.username) = :username
                        """, Trainer.class)
                .setParameter("username", normalize(username))
                .getResultStream()
                .findFirst();
    }

    public List<Trainer> findAll() {
        return entityManager.createQuery("""
                        select tr
                        from Trainer tr
                        join fetch tr.user
                        join fetch tr.specialization
                        order by tr.id
                        """, Trainer.class)
                .getResultList();
    }

    public List<Trainer> findAllByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        List<String> normalized = usernames.stream().map(this::normalize).toList();
        return entityManager.createQuery("""
                        select tr
                        from Trainer tr
                        join fetch tr.user u
                        join fetch tr.specialization
                        where lower(u.username) in :usernames
                        order by tr.id
                        """, Trainer.class)
                .setParameter("usernames", normalized)
                .getResultList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
