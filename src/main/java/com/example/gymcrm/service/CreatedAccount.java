package com.example.gymcrm.service;

public record CreatedAccount<T>(T profile, String rawPassword) {
}
