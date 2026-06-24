package com.ticketflow.service;

import com.ticketflow.entity.Pay;
import com.ticketflow.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final PayRepository payRepository;
    private final BookingService bookingService;

    // 🌟 주기적으로 실행되는 타이머 (fixedRate = 60000은 60초마다 한 번씩 실행된다는 뜻입니다)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredBookings() {

        // 1. 기준 시간 설정: 지금으로부터 30분 전
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(30);

        // 2. 30분이 지났는데 결제가 안 된 내역(좀비 예매)들을 DB에서 싹 쓸어옵니다.
        List<Pay> expiredPays = payRepository.findExpiredPendingPayments(thresholdTime);

        if (!expiredPays.isEmpty()) {
            System.out.println("🧹 [스케줄러] 30분 경과 미결제 내역 " + expiredPays.size() + "건 청소 시작!");

            for (Pay pay : expiredPays) {
                try {
                    // ① 결제 상태를 '시간 초과 실패'로 낙인찍습니다.
                    pay.setPayStatus("FAILED");
                    pay.setPayFailReason("결제 대기 시간(30분) 초과 자동 취소");

                    // ② 예매에 묶여있던 좌석을 다른 사람이 살 수 있게 풀어줍니다.
                    if (pay.getReservation() != null) {
                        bookingService.releaseUnpaidSeat(pay.getReservation().getReservationKey());
                    }

                    System.out.println("✅ 예매번호 TF-0000" + pay.getPayNo() + " 자동 취소 및 좌석 회수 완료.");
                } catch (Exception e) {
                    System.err.println("🚨 예매번호 " + pay.getPayNo() + " 자동 취소 중 에러: " + e.getMessage());
                }
            }
        }
    }
}