package com.example.gymcrm.repository;

import com.example.gymcrm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select u
            from User u
            where lower(u.username) = :username
            """)
    Optional<User> findByNormalizedUsername(@Param("username") String username);

    @Query("""
            select count(u) > 0
            from User u
            where lower(u.username) = :username
            """)
    boolean existsByNormalizedUsername(@Param("username") String username);

    @Query("""
            select count(u)
            from User u
            where lower(u.firstName) = :firstName
              and lower(u.lastName) = :lastName
            """)
    long countByNormalizedFirstNameAndLastName(@Param("firstName") String firstName,
                                               @Param("lastName") String lastName);

    default Optional<User> findByUsername(String username) {
        return findByNormalizedUsername(UsernameNormalizer.normalize(username));
    }

    default boolean existsByUsername(String username) {
        return existsByNormalizedUsername(UsernameNormalizer.normalize(username));
    }

    default long countByFirstNameAndLastName(String firstName, String lastName) {
        return countByNormalizedFirstNameAndLastName(
                UsernameNormalizer.normalize(firstName),
                UsernameNormalizer.normalize(lastName));
    }
}
