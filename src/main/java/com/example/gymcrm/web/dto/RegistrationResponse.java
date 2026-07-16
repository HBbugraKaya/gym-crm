package com.example.gymcrm.web.dto;

public record RegistrationResponse(String username, String password) {
    @Override
    public String toString() {
        return "RegistrationResponse[username=" + username + ", password=<redacted>]";
    }
}
