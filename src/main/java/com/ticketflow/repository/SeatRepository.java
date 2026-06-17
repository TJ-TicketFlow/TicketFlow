package com.ticketflow.repository;

import com.ticketflow.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, String> {
    List<Seat> findByConcert_ConcertId(String concertId);
}
