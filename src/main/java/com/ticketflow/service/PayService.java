package com.ticketflow.service;

import com.ticketflow.entity.Pay;
import com.ticketflow.entity.User;
import com.ticketflow.repository.PayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayService {

    private final PayRepository payRepository;
    private final RefundService refundService;

    /**
     * 본인 결제 환불 요청.
     * - payNo 로 Pay 를 조회하고, 로그인 사용자가 실제 소유자인지 검증한 뒤
     *   LemonSqueezy에 "전액 환불"을 요청하고 DB 상태를 PAID -> REFUNDED 로 갱신한다.
     * - 부분 환불을 막고 항상 전액 환불만 허용하는 이유: 클라이언트가 보낸 금액을
     *   그대로 신뢰하면 사용자가 일부만 환불받고 결제는 그대로 유지하는 등
     *   금액을 조작할 위험이 있기 때문. 부분 환불이 필요하면 별도 관리자 기능으로 분리할 것.
     *
     * @param payNo      환불 대상 결제 PK (우리 DB 기준)
     * @param requester  현재 로그인한 사용자 (컨트롤러에서 @AuthenticationPrincipal 로 조회한 User)
     */
    @Transactional
    public void refundMyPayment(Long payNo, User requester) {

        Pay pay = payRepository.findById(payNo)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        // ── 소유권 검증 ──────────────────────────────────────────────
        Long ownerUserNo = pay.getReservation().getSelectedSeat().getUser().getUserNo();
        if (!ownerUserNo.equals(requester.getUserNo())) {
            throw new SecurityException("본인의 결제 내역만 환불할 수 있습니다.");
        }

        // ── 상태 검증 ────────────────────────────────────────────────
        if (!"PAID".equals(pay.getPayStatus())) {
            throw new IllegalStateException("환불 가능한 상태가 아닙니다. (현재 상태: " + pay.getPayStatus() + ")");
        }

        if (pay.getLsOrderId() == null || pay.getLsOrderId().isBlank()) {
            throw new IllegalStateException("결제 외부 주문 정보가 없어 환불을 진행할 수 없습니다.");
        }

        // ── LemonSqueezy 환불 호출 (항상 전액 환불, amount 미지정) ──────
        Map<String, Object> result = refundService.refundOrder(pay.getLsOrderId(), null);

        // ── DB 상태 갱신 ────────────────────────────────────────────
        pay.setPayStatus("REFUNDED");
        payRepository.save(pay);
    }
}