package com.example.gymcrm.repository;

import com.example.gymcrm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    default Optional<User> findByUsername(String username) {
        return findByUsernameIgnoreCase(UsernameNormalizer.trim(username));
    }

    default boolean existsByUsername(String username) {
        return existsByUsernameIgnoreCase(UsernameNormalizer.trim(username));
    }

    default long countByFirstNameAndLastName(String firstName, String lastName) {
        return countByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                UsernameNormalizer.trim(firstName),
                UsernameNormalizer.trim(lastName));
    }
}
