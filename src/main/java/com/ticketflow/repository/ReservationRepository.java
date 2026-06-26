package com.ticketflow.repository;

import com.ticketflow.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findBySelectedSeat_User_UserId(String userId);
    // 특정 공연, 특정 회차에 예약된 건수 조회
    long countBySelectedSeat_Concert_ConcertIdAndSessionTime(String concertId, String sessionTime);
}