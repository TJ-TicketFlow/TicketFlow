package com.ticketflow.repository;

import com.ticketflow.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findBySelectedSeat_User_UserId(String userId);
}
