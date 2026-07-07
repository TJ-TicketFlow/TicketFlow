package com.ticketflow.repository;

import com.ticketflow.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 기존 메서드 유지
    List<Reservation> findBySelectedSeat_User_UserId(String userId);

    // [추가] 계정당 최대 예매 매수 제한(4매)을 서버에서 강제하기 위한 집계 쿼리
    // 취소(CANCELLED)/실패(FAILED)한 결제 건은 제외하고, 결제 대기중이거나(READY, Pay 없음) 결제 완료(PAID)된
    // 예매만 "현재 보유 중인 티켓"으로 집계합니다.
    @Query("SELECT COALESCE(SUM(r.reservationCount), 0) FROM Reservation r " +
            "JOIN r.selectedSeat s " +
            "WHERE s.user.userNo = :userNo " +
            "AND s.concert.concertId = :concertId " +
            "AND NOT EXISTS (SELECT p FROM Pay p WHERE p.reservation = r AND p.payStatus IN ('CANCELLED', 'FAILED'))")
    long sumActiveTicketCountByUserAndConcert(@Param("userNo") Long userNo, @Param("concertId") String concertId);


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


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.reservationCreatedAt <= :thresholdTime " +
            "AND NOT EXISTS (SELECT p FROM Pay p WHERE p.reservation = r AND p.payStatus = 'PAID') " +
            "AND r.selectedSeat.seatState IN (1, 2)")
    List<Reservation> findExpiredAndUnpaidReservations(@Param("thresholdTime") LocalDateTime thresholdTime);
}