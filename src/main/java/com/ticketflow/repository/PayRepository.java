package com.ticketflow.repository;

import com.ticketflow.entity.Pay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayRepository extends JpaRepository<Pay, Long> {
    Optional<Pay> findByMerchantUid(String merchantUid);
    List<Pay> findByReservation_SelectedSeat_User_UserId(String userId);

    @Query("SELECT p FROM Pay p WHERE p.reservation.selectedSeat.user.userId = :userId " +
            "AND p.payCreatedAt BETWEEN :startDate AND :endDate " +
            "AND p.payStatus IN ('PAID', 'CANCELLED', 'FAILED') " +
            "ORDER BY p.payCreatedAt DESC")
    Page<Pay> findMyPaymentsByDateRange(@Param("userId") String userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    // 💡 2. [신규] 30분이 지났는데 아직 결제가 완료되지 않은(진행중인) 결제 건들 찾아내기
    @Query("SELECT p FROM Pay p WHERE p.payCreatedAt <= :thresholdTime " +
            "AND (p.payStatus IS NULL OR p.payStatus NOT IN ('PAID', 'CANCELLED', 'FAILED'))")
    List<Pay> findExpiredPendingPayments(@Param("thresholdTime") LocalDateTime thresholdTime);

    boolean existsByReservation_ReservationKeyAndPayStatus(Long reservationKey, String payStatus);
}