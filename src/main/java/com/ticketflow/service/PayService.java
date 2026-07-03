package com.ticketflow.service;

import com.ticketflow.entity.Pay;
import com.ticketflow.repository.PayRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayService {
    private final PayRepository payRepository;
    private final StatsService statsService;

    @Transactional
    public void processPaymentSuccess(Long payNo) { // concertId 인자 제거
        // 1. 기존 결제 완료 로직
        Pay pay = payRepository.findById(payNo)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
        pay.setPayStatus("SUCCESS");
        payRepository.save(pay);

        // 2. Pay 객체를 통해 안전하게 concertId 추출
        String concertId = pay.getReservation().getConcert().getConcertId();

        // 3. 통계 갱신 호출
        statsService.updateStats(concertId);
    }
}
