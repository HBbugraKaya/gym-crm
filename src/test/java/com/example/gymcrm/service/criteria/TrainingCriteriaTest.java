package com.example.gymcrm.service.criteria;

import com.example.gymcrm.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingCriteriaTest {
    private static final LocalDate EARLIER = LocalDate.of(2026, 1, 1);
    private static final LocalDate LATER = LocalDate.of(2026, 1, 2);

    @Test
    void acceptsOrderedAndOpenDateRanges() {
        assertThatCode(() -> new TraineeTrainingCriteria(EARLIER, LATER, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> new TrainerTrainingCriteria(null, LATER, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReversedDateRanges() {
        assertThatThrownBy(() -> new TraineeTrainingCriteria(LATER, EARLIER, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("periodFrom must be on or before periodTo");
        assertThatThrownBy(() -> new TrainerTrainingCriteria(LATER, EARLIER, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("periodFrom must be on or before periodTo");
    }
}
