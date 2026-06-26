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
    public void processPaymentSuccess(Long payNo, String concertId) {
        // 1. 기존 결제 완료 로직
        Pay pay = payRepository.findById(payNo).orElseThrow();
        pay.setPayStatus("SUCCESS");
        payRepository.save(pay);

        // 2. 통계 갱신 호출
        statsService.updateStats(concertId);
    }
}
