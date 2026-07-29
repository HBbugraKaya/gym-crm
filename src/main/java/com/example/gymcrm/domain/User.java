package com.example.gymcrm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@ToString(exclude = "password")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String firstName;

    @Setter
    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, updatable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Setter
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private Instant lockedUntil;

    public User(String firstName, String lastName, String username, String password, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.active = active;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void clearExpiredLock(Instant now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            resetFailedLoginAttempts();
        }
    }

    public void recordFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
