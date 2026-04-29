package com.connectsphere.authservice.repository;

import com.connectsphere.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);


    List<User> findByUsernameStartingWithIgnoreCase(String prefix);

    List<User> findByFullNameContainingIgnoreCase(String fullName);
}