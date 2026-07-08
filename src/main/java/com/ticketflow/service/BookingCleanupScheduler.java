package com.ticketflow.service;

import com.ticketflow.entity.Pay;
import com.ticketflow.entity.Reservation;
import com.ticketflow.repository.PayRepository;
import com.ticketflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    // 두 개의 창고와 서비스 도구를 모두 가져옵니다.
    private final ReservationRepository reservationRepository;
    private final PayRepository payRepository;
    private final BookingService bookingService;
    private static final long PAY_TIMEOUT_MINUTES = 10;

    // 30초(30,000ms)마다 주기적으로 자동 실행
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanupExpiredBookings() {

        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);

        // ==============================================================
        // 작전 1: 개발자님의 쿼리를 활용한 '결제 장부(Pay)' 청소
        // ==============================================================
        List<Pay> expiredPays = payRepository.findExpiredPendingPayments(thresholdTime);

        if (!expiredPays.isEmpty()) {

            for (Pay pay : expiredPays) {
                // 1) 결제 상태를 실패로 변경
                pay.setPayStatus("FAILED");
                pay.setPayFailReason("결제 대기 시간("+ PAY_TIMEOUT_MINUTES +"분) 초과 자동 취소");

                // 2) 묶인 좌석을 구출!
                if (pay.getReservation() != null) {
                    bookingService.releaseUnpaidSeat(pay.getReservation().getReservationKey());
                }
            }
        }

        // ==============================================================
        // 작전 2: 아예 결제 버튼조차 안 누르고 도망친 '예약(Reservation)' 청소
        // ==============================================================
        // 💡 주의: 아까 ReservationRepository에 추가했던 쿼리입니다.
        List<Reservation> abandonedReservations = reservationRepository.findExpiredAndUnpaidReservations(thresholdTime);

        if (!abandonedReservations.isEmpty()) {

            for (Reservation reservation : abandonedReservations) {
                try {
                    // 예약 장부만 덩그러니 남았으므로, 좌석만 바로 풀어버리면 됩니다.
                    bookingService.releaseUnpaidSeat(reservation.getReservationKey());
                } catch (Exception e) {
                    System.err.println("🚨 예약번호 " + reservation.getReservationKey() + " 좌석 회수 중 에러: " + e.getMessage());
                }
            }
        }
    }
}