package com.example.gymcrm.domain;

import org.junit.jupiter.api.Test;

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
        assertThat(trainee.toString()).contains("A.B").doesNotContain("secret1234");
        assertThat(second.toString()).contains("E.F").doesNotContain("secret1234");
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
        assertThat(training.toString()).contains("Cardio").doesNotContain("newPassword");
        assertThat(type.toString()).contains("CARDIO");
    }
}
