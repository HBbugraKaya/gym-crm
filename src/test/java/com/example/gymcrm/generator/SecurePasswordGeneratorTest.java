package com.example.gymcrm.generator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurePasswordGeneratorTest {
    @Test
    void generatesTenCharacterAlphanumericPasswords() {
        SecurePasswordGenerator generator = new SecurePasswordGenerator();

        String password = generator.generate();

        assertThat(password).hasSize(10);
        assertThat(password).matches("[A-Za-z0-9]+");
    }
}
