package com.example.gymcrm.utility;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generatePassword() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int passwordLength = 10;
        StringBuilder password = new StringBuilder(passwordLength);
        for (int i = 0; i < passwordLength; i++) {
            password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return password.toString();
    }
}
