package com.ticketflow.repository;

import com.ticketflow.entity.SelectedSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelectedSeatRepository extends JpaRepository<SelectedSeat, Long> {
    List<SelectedSeat> findByUser_UserId(String userId);

    List<SelectedSeat> findByConcert_ConcertIdAndSeatState(String concertId, Short seatState);
    long countByConcert_ConcertId(String concertId);

}
