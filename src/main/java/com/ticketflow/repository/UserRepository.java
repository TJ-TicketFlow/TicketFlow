package com.ticketflow.repository;

import com.ticketflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByUserEmail(String email);
    Optional<User> findByUserNameAndUserEmail(String name, String email);
    boolean existsByUserId(String userId);
    boolean existsByUserEmail(String email);
}
