package com.ticketflow.repository;

import com.ticketflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByUserEmail(String userEmail);

    Optional<User> findByUserEmail(String userEmail);

    // 아이디 찾기: 이름 + 이메일 일치 회원 조회
    boolean existsByUserNameAndUserEmail(String userName, String userEmail);

    Optional<User> findByUserNameAndUserEmail(String userName, String userEmail);

    // 비밀번호 찾기: 아이디 + 이메일 일치 여부
    boolean existsByUserIdAndUserEmail(String userId, String userEmail);
}