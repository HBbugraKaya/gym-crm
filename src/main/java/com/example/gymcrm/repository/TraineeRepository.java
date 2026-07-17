package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    Optional<Trainee> findByUserUsernameIgnoreCase(String username);

    /**
     * Loads trainers graph in one query. {@code Distinct} avoids duplicate roots when joining the collection.
     */
    @EntityGraph(attributePaths = {"trainers", "trainers.user", "trainers.specialization"})
    Optional<Trainee> findDistinctByUserUsernameIgnoreCase(String username);

    default Optional<Trainee> findByUsername(String username) {
        return findByUserUsernameIgnoreCase(UsernameNormalizer.trim(username));
    }

    default Optional<Trainee> findByUsernameWithTrainers(String username) {
        return findDistinctByUserUsernameIgnoreCase(UsernameNormalizer.trim(username));
    }
}
