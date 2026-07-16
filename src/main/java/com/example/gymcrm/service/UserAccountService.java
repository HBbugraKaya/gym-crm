package com.example.gymcrm.service;

import com.example.gymcrm.domain.User;
import com.example.gymcrm.service.command.Credentials;

public interface UserAccountService {
    User authenticate(Credentials credentials);

    void changePassword(Credentials credentials, String targetUsername, String newPassword);

    User changeStatus(Credentials credentials, String targetUsername, boolean active);
}
