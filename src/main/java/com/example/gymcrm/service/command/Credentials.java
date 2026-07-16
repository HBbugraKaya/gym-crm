package com.example.gymcrm.service.command;

public record Credentials(String username, String password) {
    @Override
    public String toString() {
        return "Credentials[username=" + username + ", password=[REDACTED]]";
    }
}
