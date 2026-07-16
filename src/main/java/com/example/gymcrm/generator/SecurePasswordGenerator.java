package com.example.gymcrm.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecurePasswordGenerator implements PasswordGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
