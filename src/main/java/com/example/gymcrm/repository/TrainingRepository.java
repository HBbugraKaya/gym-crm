package com.example.gymcrm.repository;

import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingTypeName;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    @EntityGraph(attributePaths = {"trainer.user", "trainingType"})
    @Query("""
            select training from Training training
            where lower(training.trainee.user.username) = lower(:username)
              and (:fromDate is null or training.date >= :fromDate)
              and (:toDate is null or training.date <= :toDate)
              and (:trainerName is null or lower(concat(training.trainer.user.firstName, ' ', training.trainer.user.lastName))
                   like lower(concat('%', :trainerName, '%')))
              and (:trainingType is null or training.trainingType.name = :trainingType)
            """)
    List<Training> findTraineeTrainings(
            @Param("username") String username,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("trainerName") String trainerName,
            @Param("trainingType") TrainingTypeName trainingType);

    @EntityGraph(attributePaths = {"trainee.user", "trainingType"})
    @Query("""
            select training from Training training
            where lower(training.trainer.user.username) = lower(:username)
              and (:fromDate is null or training.date >= :fromDate)
              and (:toDate is null or training.date <= :toDate)
              and (:traineeName is null or lower(concat(training.trainee.user.firstName, ' ', training.trainee.user.lastName))
                   like lower(concat('%', :traineeName, '%')))
            """)
    List<Training> findTrainerTrainings(
            @Param("username") String username,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("traineeName") String traineeName);
}
