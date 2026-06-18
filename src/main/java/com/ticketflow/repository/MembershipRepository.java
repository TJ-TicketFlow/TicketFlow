package com.ticketflow.repository;

import com.ticketflow.entity.Membership;
import com.ticketflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findTopByUser_UserIdOrderByMembershipCreatedAtDesc(String userId);
    List<Membership> findByUser(User user);
    Optional<Membership> findByUser_UserEmail(String userEmail);
}
