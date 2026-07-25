package com.example.gymcrm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gymcrm.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
