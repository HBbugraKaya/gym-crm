package com.example.gymcrm.web.mapper;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.TrainingTypeName;
import com.example.gymcrm.domain.User;
import com.example.gymcrm.service.CreatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GymWebMapperTest {
    private GymWebMapper mapper;
    private TrainingType yoga;
    private Trainee trainee;
    private Trainer trainer;

    @BeforeEach
    void setUp() {
        mapper = new GymWebMapper();
        yoga = new TrainingType(TrainingTypeName.YOGA);
        trainee = new Trainee(
                new User("John", "Smith", "john.smith", "encoded-trainee-password", true),
                LocalDate.of(2001, 1, 1),
                "Istanbul");
        trainer = new Trainer(
                new User("Alice", "Coach", "alice.coach", "encoded-trainer-password", true),
                yoga);
        trainee.assignTrainer(trainer);
    }

    @Test
    void mapsTraineeRegistrationCredentialsFromCreatedAccount() {
        var response = mapper.toRegistrationResponse(new CreatedAccount<>(trainee, "trainee-password"));

        assertThat(response.username()).isEqualTo("john.smith");
        assertThat(response.password()).isEqualTo("trainee-password");
    }

    @Test
    void mapsTrainerRegistrationCredentialsFromCreatedAccount() {
        var response = mapper.toRegistrationResponse(new CreatedAccount<>(trainer, "trainer-password"));

        assertThat(response.username()).isEqualTo("alice.coach");
        assertThat(response.password()).isEqualTo("trainer-password");
    }

    @Test
    void mapsTraineeProfileIncludingTrainerDetails() {
        var response = mapper.toTraineeProfile(trainee);

        assertThat(response.username()).isEqualTo("john.smith");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(2001, 1, 1));
        assertThat(response.address()).isEqualTo("Istanbul");
        assertThat(response.active()).isTrue();
        assertThat(response.trainers()).singleElement().satisfies(summary -> {
            assertThat(summary.username()).isEqualTo("alice.coach");
            assertThat(summary.specialization()).isEqualTo(TrainingTypeName.YOGA);
        });
    }

    @Test
    void mapsTrainerProfileIncludingTraineeDetails() {
        var response = mapper.toTrainerProfile(trainer);

        assertThat(response.username()).isEqualTo("alice.coach");
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Coach");
        assertThat(response.specialization()).isEqualTo(TrainingTypeName.YOGA);
        assertThat(response.active()).isTrue();
        assertThat(response.trainees()).singleElement().satisfies(summary -> {
            assertThat(summary.username()).isEqualTo("john.smith");
            assertThat(summary.firstName()).isEqualTo("John");
            assertThat(summary.lastName()).isEqualTo("Smith");
        });
    }

    @Test
    void mapsTrainerSummaryAndSummaryList() {
        var summary = mapper.toTrainerSummary(trainer);
        var summaries = mapper.toTrainerSummaries(List.of(trainer));

        assertThat(summary.username()).isEqualTo("alice.coach");
        assertThat(summary.firstName()).isEqualTo("Alice");
        assertThat(summary.lastName()).isEqualTo("Coach");
        assertThat(summary.specialization()).isEqualTo(TrainingTypeName.YOGA);
        assertThat(summaries).containsExactly(summary);
    }

    @Test
    void mapsTraineeSummary() {
        var summary = mapper.toTraineeSummary(trainee);

        assertThat(summary.username()).isEqualTo("john.smith");
        assertThat(summary.firstName()).isEqualTo("John");
        assertThat(summary.lastName()).isEqualTo("Smith");
    }

    @Test
    void mapsTrainingForTraineeAndTrainerViews() {
        LocalDate date = LocalDate.of(2026, 7, 16);
        var training = new Training(trainee, trainer, "Morning yoga", yoga, date, 60);

        var traineeView = mapper.toTraineeTrainings(List.of(training));
        var trainerView = mapper.toTrainerTrainings(List.of(training));

        assertThat(traineeView).singleElement().satisfies(response -> {
            assertThat(response.trainingName()).isEqualTo("Morning yoga");
            assertThat(response.trainingDate()).isEqualTo(date);
            assertThat(response.trainingType()).isEqualTo(TrainingTypeName.YOGA);
            assertThat(response.durationMinutes()).isEqualTo(60);
            assertThat(response.trainerName()).isEqualTo("Alice Coach");
        });
        assertThat(trainerView).singleElement().satisfies(response -> {
            assertThat(response.trainingName()).isEqualTo("Morning yoga");
            assertThat(response.trainingDate()).isEqualTo(date);
            assertThat(response.trainingType()).isEqualTo(TrainingTypeName.YOGA);
            assertThat(response.durationMinutes()).isEqualTo(60);
            assertThat(response.traineeName()).isEqualTo("John Smith");
        });
    }

    @Test
    void mapsTrainingTypeIdAndName() throws ReflectiveOperationException {
        setId(yoga, 42L);

        var responses = mapper.toTrainingTypes(List.of(yoga));

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.name()).isEqualTo(TrainingTypeName.YOGA);
        });
    }

    private void setId(TrainingType trainingType, Long id) throws ReflectiveOperationException {
        Field idField = TrainingType.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(trainingType, id);
    }
}
