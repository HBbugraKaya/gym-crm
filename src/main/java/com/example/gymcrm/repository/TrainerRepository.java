package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    Optional<Trainer> findByUserUsernameIgnoreCase(String username);

    @Query("SELECT t FROM Trainer t WHERE t.user.active = true AND t NOT IN (SELECT tr FROM Trainee trainee JOIN trainee.trainers tr WHERE lower(trainee.user.username) = lower(:traineeUsername))")
    List<Trainer> findUnassignedActiveTrainers(@Param("traineeUsername") String traineeUsername);

    List<Trainer> findByUserUsernameIgnoreCaseIn(Collection<String> usernames);

    default Optional<Trainer> findByUsername(String username) {
        return findByUserUsernameIgnoreCase(UsernameNormalizer.trim(username));
    }

    default List<Trainer> findAllByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        List<String> trimmed = usernames.stream().map(UsernameNormalizer::trim).toList();
        return findByUserUsernameIgnoreCaseIn(trimmed);
    }
}
