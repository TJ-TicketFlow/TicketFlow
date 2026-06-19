package com.ticketflow.service;

import com.ticketflow.entity.MembershipPayments;
import com.ticketflow.entity.User;
import com.ticketflow.repository.MembershipPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 멤버십 정기결제(구독 인보이스) 환불 처리.
 * 일회성 주문 환불은 PayService.refundMyPayment() 를 사용하고,
 * 멤버십 정기결제 환불은 이 서비스를 사용한다 (엔드포인트/type 값이 다르기 때문).
 */
@Service
@RequiredArgsConstructor
public class MembershipPaymentRefundService {

    private final MembershipPaymentRepository membershipPaymentRepository;
    private final RefundService refundService;

    /**
     * @param paymentId 환불 대상 membership_payments PK
     * @param requester 현재 로그인한 사용자
     */
    @Transactional
    public void refundMyMembershipPayment(Long paymentId, User requester) {

        MembershipPayments payment = membershipPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        // ── 소유권 검증 ──────────────────────────────────────────────
        // Membership 엔티티에 user 필드가 명확히 존재하므로 이 부분은 그대로 사용 가능.
        Long ownerUserNo = payment.getMembership().getUser().getUserNo();
        if (!ownerUserNo.equals(requester.getUserNo())) {
            throw new SecurityException("본인의 결제 내역만 환불할 수 있습니다.");
        }

        // ── 상태 검증 ────────────────────────────────────────────────
        if (!"PAID".equals(payment.getPaymentStatus())) {
            throw new IllegalStateException("환불 가능한 상태가 아닙니다. (현재 상태: " + payment.getPaymentStatus() + ")");
        }

        if (payment.getMembershipOrderId() == null || payment.getMembershipOrderId().isBlank()) {
            throw new IllegalStateException("결제 외부 주문 정보가 없어 환불을 진행할 수 없습니다.");
        }

        // ── LemonSqueezy 환불 호출 (항상 전액 환불) ─────────────────────
        // 주의: membershipOrderId 에 실제로 LemonSqueezy의 subscription-invoice id가
        // 저장되고 있는지 webhook 처리 로직(WebhookRequestDto 수신부)에서 확인 필요.
        Map<String, Object> result = refundService.refundSubscriptionInvoice(payment.getMembershipOrderId(), null);

        // ── DB 상태 갱신 ────────────────────────────────────────────
        payment.setPaymentStatus("REFUNDED");
        membershipPaymentRepository.save(payment);
    }
}
