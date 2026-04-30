package com.example.multi_stores.repository;

import com.example.multi_stores.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    boolean existsByEmail(String email);

    long deleteByEmailVerifiedFalseAndVerificationTokenExpiryBefore(Instant cutoff);
}
