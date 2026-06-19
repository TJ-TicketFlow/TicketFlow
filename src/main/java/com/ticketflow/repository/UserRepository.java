package com.ticketflow.repository;

import com.ticketflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByUserEmail(String userEmail);

    Optional<User> findByUserEmail(String userEmail);

    Optional<User> findByUserNameAndUserEmail(String userName, String userEmail);

    Optional<User> findByUserIdAndUserEmail(String userId, String userEmail);
}
