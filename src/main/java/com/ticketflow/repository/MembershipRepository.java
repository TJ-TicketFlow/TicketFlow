package com.ticketflow.repository;

import com.ticketflow.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findTopByUser_UserIdOrderByMembershipCreatedAtDesc(String userId);
}
