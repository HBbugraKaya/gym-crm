package com.example.gymcrm.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {
    @Test
    void traineeMaintainsBidirectionalTrainerAssignments() {
        TrainingType type = new TrainingType(TrainingTypeName.YOGA);
        Trainee trainee = new Trainee(new User("A", "B", "A.B", "secret1234", true),
                LocalDate.of(2000, 1, 1), "Address");
        Trainer first = new Trainer(new User("C", "D", "C.D", "secret1234", true), type);
        Trainer second = new Trainer(new User("E", "F", "E.F", "secret1234", true), type);

        trainee.assignTrainer(first);
        trainee.replaceTrainers(List.of(second));

        assertThat(trainee.getTrainers()).containsExactly(second);
        assertThat(first.getTrainees()).isEmpty();
        assertThat(second.getTrainees()).containsExactly(trainee);
    }

    @Test
    void userAndTrainingExposeExpectedFieldsWithoutSensitiveToStringData() {
        User user = new User("First", "Last", "First.Last", "password12", true);
        TrainingType type = new TrainingType(TrainingTypeName.CARDIO);
        Trainee trainee = new Trainee(user, null, null);
        Trainer trainer = new Trainer(new User("Trainer", "One", "Trainer.One", "password34", true), type);
        Training training = new Training(trainee, trainer, "Cardio", type, LocalDate.of(2026, 1, 1), 30);

        user.changePassword("newPassword");
        user.setActive(false);

        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getLastName()).isEqualTo("Last");
        assertThat(user.getPassword()).isEqualTo("newPassword");
        assertThat(user.isActive()).isFalse();
        assertThat(training.getTrainee()).isSameAs(trainee);
        assertThat(training.getTrainer()).isSameAs(trainer);
        assertThat(training.getTrainingType()).isSameAs(type);
        assertThat(training.getDurationMinutes()).isEqualTo(30);
        assertThat(user.toString()).doesNotContain("newPassword");
    }

    @Test
    void userLocksAfterTheConfiguredNumberOfFailedLoginsAndResetsAfterExpiry() {
        User user = new User("First", "Last", "First.Last", "password12", true);
        Instant now = Instant.parse("2026-07-29T10:00:00Z");

        user.recordFailedLogin(now, 3, Duration.ofMinutes(5));
        user.recordFailedLogin(now, 3, Duration.ofMinutes(5));
        assertThat(user.isLockedAt(now)).isFalse();

        user.recordFailedLogin(now, 3, Duration.ofMinutes(5));
        assertThat(user.isLockedAt(now.plusSeconds(1))).isTrue();

        user.clearExpiredLock(now.plus(Duration.ofMinutes(5)));
        assertThat(user.isLockedAt(now.plus(Duration.ofMinutes(5)))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}
