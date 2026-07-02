package com.example.gymcrm.service;

import com.example.gymcrm.exception.ValidationException;

import java.util.Collection;

public final class ValidationSupport {
    private ValidationSupport() {
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " must not be null");
        }
        return value;
    }

    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
        return value;
    }

    public static <T extends Collection<?>> T requireNotEmpty(T values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new ValidationException(fieldName + " must not be empty");
        }
        return values;
    }
}
