package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Trainee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @Query("""
            select t
            from Trainee t
            join fetch t.user u
            where lower(u.username) = :username
            """)
    Optional<Trainee> findByNormalizedUsername(@Param("username") String username);

    @Query("""
            select distinct t
            from Trainee t
            join fetch t.user u
            left join fetch t.trainers tr
            left join fetch tr.user tru
            left join fetch tr.specialization
            where lower(u.username) = :username
            """)
    Optional<Trainee> findByNormalizedUsernameWithTrainers(@Param("username") String username);

    @Override
    @Query("""
            select t
            from Trainee t
            join fetch t.user
            order by t.id
            """)
    List<Trainee> findAll();

    default Optional<Trainee> findByUsername(String username) {
        return findByNormalizedUsername(UsernameNormalizer.normalize(username));
    }

    default Optional<Trainee> findByUsernameWithTrainers(String username) {
        return findByNormalizedUsernameWithTrainers(UsernameNormalizer.normalize(username));
    }
}
