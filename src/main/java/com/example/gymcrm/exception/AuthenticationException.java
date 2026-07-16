package com.example.gymcrm.exception;

public class AuthenticationException extends GymCrmException {
    public AuthenticationException(String profileType) {
        super(profileType + " authentication failed");
    }
}
