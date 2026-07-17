package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;

public interface UserAccountService {
    void changePassword(String targetUsername, String newPassword);

    User changeStatus(String targetUsername, boolean active);
}
