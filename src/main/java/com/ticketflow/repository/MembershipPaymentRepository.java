package com.ticketflow.repository;

import com.ticketflow.entity.Membership;
import com.ticketflow.entity.MembershipPayments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayments, Long> {
    List<MembershipPayments> findByMembershipOrderByMembershipHistoryDateDesc(Membership membership);
    List<MembershipPayments> findByMembershipAndPaymentStatusOrderByMembershipHistoryDateDesc(Membership membership, String paymentStatus);
}