package com.example.gymcrm.repository;

import com.example.gymcrm.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class UserRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery("""
                        select u
                        from User u
                        where lower(u.username) = :username
                        """, User.class)
                .setParameter("username", normalize(username))
                .getResultStream()
                .findFirst();
    }

    public boolean existsByUsername(String username) {
        return entityManager.createQuery("""
                        select count(u)
                        from User u
                        where lower(u.username) = :username
                        """, Long.class)
                .setParameter("username", normalize(username))
                .getSingleResult() > 0;
    }

    public long countByFirstNameAndLastName(String firstName, String lastName) {
        return entityManager.createQuery("""
                        select count(u)
                        from User u
                        where lower(u.firstName) = :firstName
                          and lower(u.lastName) = :lastName
                        """, Long.class)
                .setParameter("firstName", normalize(firstName))
                .setParameter("lastName", normalize(lastName))
                .getSingleResult();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
