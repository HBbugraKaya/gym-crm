package com.example.gymcrm.repository;

import java.util.Locale;

final class UsernameNormalizer {

    private UsernameNormalizer() {
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + normalize(value) + "%";
    }
}
