package com.example.gymcrm.web.dto;

import com.example.gymcrm.domain.TrainingTypeName;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void registrationRequestsRejectMissingRequiredFields() {
        var traineeRequest = new TraineeRegistrationRequest(" ", null, LocalDate.now().plusDays(1), null);
        var trainerRequest = new TrainerRegistrationRequest("Alice", " ", null);

        assertThat(validator.validate(traineeRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("firstName", "lastName", "dateOfBirth");
        assertThat(validator.validate(trainerRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("lastName", "specialization");
    }

    @Test
    void profileUpdatesRequireNamesAndExplicitStatus() {
        var traineeRequest = new UpdateTraineeRequest("", "Smith", null, null, null);
        var trainerRequest = new UpdateTrainerRequest("Alice", "", null);

        assertThat(validator.validate(traineeRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("firstName", "active");
        assertThat(validator.validate(trainerRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("lastName", "active");
    }

    @Test
    void commandRequestsRejectMissingOrUnsafeValues() {
        var training = new AddTrainingRequest("", " ", "", null, 0);
        var assignments = new TrainerAssignmentsRequest(List.of("valid.trainer", " "));
        var missingAssignments = new TrainerAssignmentsRequest(null);
        var password = new ChangePasswordRequest(" ");
        var status = new ChangeStatusRequest(null);
        var login = new LoginRequest(" ", "");

        assertThat(validator.validate(training))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("traineeUsername", "trainerUsername", "trainingName", "trainingDate", "durationMinutes");
        assertThat(validator.validate(assignments))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("trainerUsernames[1].<list element>");
        assertThat(validator.validate(missingAssignments))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("trainerUsernames");
        assertThat(validator.validate(password))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword");
        assertThat(validator.validate(status))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("active");
        assertThat(validator.validate(login))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "password");
    }

    @Test
    void completeRequestsPassValidation() {
        LocalDate birthDate = LocalDate.of(2000, 1, 1);
        LocalDate trainingDate = LocalDate.of(2026, 7, 16);

        assertThat(validator.validate(
                new TraineeRegistrationRequest("John", "Smith", birthDate, "Istanbul"))).isEmpty();
        assertThat(validator.validate(
                new TrainerRegistrationRequest("Alice", "Coach", TrainingTypeName.YOGA))).isEmpty();
        assertThat(validator.validate(
                new UpdateTraineeRequest("John", "Smith", birthDate, "Ankara", true))).isEmpty();
        assertThat(validator.validate(
                new UpdateTrainerRequest("Alice", "Coach", false))).isEmpty();
        assertThat(validator.validate(
                new AddTrainingRequest("john.smith", "alice.coach", "Yoga", trainingDate, 60))).isEmpty();
        assertThat(validator.validate(new TrainerAssignmentsRequest(List.of("alice.coach")))).isEmpty();
        assertThat(validator.validate(new ChangePasswordRequest("new-secret"))).isEmpty();
        assertThat(validator.validate(new ChangeStatusRequest(true))).isEmpty();
        assertThat(validator.validate(new LoginRequest("john.smith", "secret"))).isEmpty();
    }
}
