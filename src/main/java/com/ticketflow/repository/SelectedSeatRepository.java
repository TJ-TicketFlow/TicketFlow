package com.ticketflow.repository;

import com.ticketflow.entity.SelectedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface SelectedSeatRepository extends JpaRepository<SelectedSeat, Long> {
    List<SelectedSeat> findByUser_UserId(String userId); //문제시 long으로 수정

    List<SelectedSeat> findByConcert_ConcertIdAndSeatState(String concertId, Short seatState);
    long countByConcert_ConcertId(String concertId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SelectedSeat s SET s.seatState = 0 WHERE s.selectedSeatId = :selectedSeatId AND (s.seatState = 1 OR s.seatState = 2)")
    int releaseSeatDirectly(@Param("selectedSeatId") Long selectedSeatId);

}
