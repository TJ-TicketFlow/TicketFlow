package com.ticketflow.repository;

import com.ticketflow.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 기존 메서드 유지
    List<Reservation> findBySelectedSeat_User_UserId(String userId);

    // [수정] 메서드 이름으로 JPA가 찾지 못하게 하고, 직접 JPQL 쿼리를 작성합니다.
    @Query("SELECT count(r) FROM Reservation r " +
            "JOIN r.selectedSeat s " +
            "JOIN s.seat st " +
            "WHERE s.concert.concertId = :concertId " +
            "AND r.sessionTime = :sessionTime " +
            "AND r.reservationDate = :reservationDate " +
            "AND st.seatClass = :seatClass")
    long countBySeatClass(@Param("concertId") String concertId,
                          @Param("sessionTime") String sessionTime,
                          @Param("reservationDate") LocalDate reservationDate,
                          @Param("seatClass") String seatClass);
}