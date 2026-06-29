package com.ticketflow.repository;

import com.ticketflow.entity.Membership;
import com.ticketflow.entity.MembershipPayments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayments, Long> {
    Page<MembershipPayments> findByMembershipOrderByMembershipHistoryDateDesc(Membership membership, Pageable pageable);
    List<MembershipPayments> findByMembershipAndPaymentStatusOrderByMembershipHistoryDateDesc(Membership membership, String paymentStatus);
}