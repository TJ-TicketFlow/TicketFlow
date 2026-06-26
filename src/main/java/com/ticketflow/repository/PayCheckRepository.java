package com.ticketflow.repository;

import com.ticketflow.entity.Pay;
import com.ticketflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PayCheckRepository extends JpaRepository<Pay, Long> {

    @Query("SELECT COUNT(p) > 0 FROM Pay p WHERE p.reservation.selectedSeat.user = :user AND p.payStatus = 'PAID' AND p.payCreatedAt >= :since")
    boolean existsPaidReservationByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);
}