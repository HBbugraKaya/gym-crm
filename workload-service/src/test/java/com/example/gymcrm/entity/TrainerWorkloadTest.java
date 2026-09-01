package com.example.gymcrm.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrainerWorkloadTest {

    private static TrainerWorkload blank() {
        return new TrainerWorkload("jane.doe", "Jane", "Doe", true, List.of());
    }

    @Test
    void addOnEmptyCreatesYearAndMonth() {
        assertEquals(60, blank().adjust("Jane", "Doe", true, 2026, 8, 60).duration(2026, 8));
    }

    @Test
    void addAccumulatesSameMonth() {
        var result = blank()
                .adjust("Jane", "Doe", true, 2026, 8, 60)
                .adjust("Jane", "Doe", true, 2026, 8, 30);
        assertEquals(90, result.duration(2026, 8));
    }

    @Test
    void deleteFloorsAtZero() {
        var result = blank()
                .adjust("Jane", "Doe", true, 2026, 8, 30)
                .adjust("Jane", "Doe", true, 2026, 8, -50);
        assertEquals(0, result.duration(2026, 8));
    }

    @Test
    void deleteOnMissingMonthDoesNotCreateAYear() {
        assertTrue(blank().adjust("Jane", "Doe", true, 2026, 8, -30).years().isEmpty());
    }

    @Test
    void identityFieldsAreReplaced() {
        var result = blank().adjust("Janet", "Doer", false, 2026, 8, 10);
        assertEquals("jane.doe", result.username());
        assertEquals("Janet", result.firstName());
        assertEquals("Doer", result.lastName());
        assertEquals(false, result.status());
    }
}
