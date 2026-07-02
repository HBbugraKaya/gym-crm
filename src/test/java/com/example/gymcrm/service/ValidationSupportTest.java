package com.example.gymcrm.service;

import com.example.gymcrm.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationSupportTest {
    @Test
    void trimsRequiredAndOptionalText() {
        assertThat(ValidationSupport.requireText("  value  ", "field")).isEqualTo("value");
        assertThat(ValidationSupport.optionalText("  value  ")).isEqualTo("value");
        assertThat(ValidationSupport.optionalText("  ")).isNull();
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> ValidationSupport.requireText(" ", "field"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ValidationSupport.requireNonNull(null, "field"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ValidationSupport.requirePositive(0, "duration"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ValidationSupport.requireNotEmpty(List.of(), "items"))
                .isInstanceOf(ValidationException.class);
    }
}
