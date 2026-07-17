package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    @Query("""
            select tr
            from Trainer tr
            join fetch tr.user u
            join fetch tr.specialization
            where lower(u.username) = :username
            """)
    Optional<Trainer> findByNormalizedUsername(@Param("username") String username);

    @Override
    @Query("""
            select tr
            from Trainer tr
            join fetch tr.user
            join fetch tr.specialization
            order by tr.id
            """)
    List<Trainer> findAll();

    @Query("""
            select tr
            from Trainer tr
            join fetch tr.user u
            join fetch tr.specialization
            where lower(u.username) in :usernames
            order by tr.id
            """)
    List<Trainer> findAllByNormalizedUsernames(@Param("usernames") Collection<String> usernames);

    default Optional<Trainer> findByUsername(String username) {
        return findByNormalizedUsername(UsernameNormalizer.normalize(username));
    }

    default List<Trainer> findAllByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        List<String> normalized = usernames.stream().map(UsernameNormalizer::normalize).toList();
        return findAllByNormalizedUsernames(normalized);
    }
}
