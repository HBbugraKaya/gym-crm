package com.example.gymcrm.service;

import com.example.gymcrm.exception.ValidationException;

final class SelfAccess {

    private SelfAccess() {
    }

    static void require(String authenticatedUsername, String requestedUsername, String message) {
        if (!authenticatedUsername.equalsIgnoreCase(requestedUsername)) {
            throw new ValidationException(message);
        }
    }
}
