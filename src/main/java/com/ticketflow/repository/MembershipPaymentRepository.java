package com.ticketflow.repository;

import com.ticketflow.entity.MembershipPayments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayments, Long> {
}