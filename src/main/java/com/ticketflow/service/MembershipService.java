package com.ticketflow.service;

import com.ticketflow.dto.RefundEligibilityDto;
import com.ticketflow.dto.WebhookRequestDto;
import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPaymentRepository paymentRepository;
    private final MembershipHistoryRepository historyRepository;
    private final UserCouponRepository userCouponRepository;
    private final PayCheckRepository payCheckRepository;
    private final LemonSqueezyRefundService lemonSqueezyRefundService;
    private final CouponService couponService;
    public void processPaymentWebhook(WebhookRequestDto dto) {
        if (dto.getData() == null || dto.getData().getAttributes() == null) {
            throw new IllegalArgumentException("웹훅 데이터가 비어 있습니다.");
        }

        String eventName = dto.getMeta() != null ? dto.getMeta().getEvent_name() : null;
        var attrs = dto.getData().getAttributes();

        User user = userRepository.findByUserEmail(attrs.getUser_email())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없음: " + attrs.getUser_email()));

        if (eventName == null) {
            System.out.println("⚠️ event_name이 없어서 처리를 건너뜁니다.");
            return;
        }

        switch (eventName) {
            case "subscription_created":
            case "subscription_updated":
            case "subscription_cancelled":
            case "subscription_resumed":
            case "subscription_expired":
            case "subscription_paused":
            case "subscription_unpaused":
                handleSubscriptionStatusEvent(user, dto, attrs);
                break;

            case "subscription_payment_success":
            case "subscription_payment_recovered":
                handlePaymentEvent(user, dto, attrs, "PAID");
                break;

            case "subscription_payment_failed":
                handlePaymentEvent(user, dto, attrs, "FAILED");
                break;

            case "subscription_payment_refunded":
                handlePaymentEvent(user, dto, attrs, "REFUNDED");
                break;

            case "order_created":
            case "order_refunded":
                System.out.println("ℹ️ Order 이벤트는 참고용으로만 로그 처리: " + eventName);
                break;

            default:
                System.out.println("ℹ️ 처리하지 않는 이벤트: " + eventName);
        }

        System.out.println("🎉 웹훅 처리 완료! (event: " + eventName + ")");
    }

    // ── 구독 상태 변경 이벤트 처리 ──
    private void handleSubscriptionStatusEvent(User user, WebhookRequestDto dto, WebhookRequestDto.WebhookAttributes attrs) {
        Instant periodEndInstant = parseInstantOrNull(attrs.getRenews_at());
        if (periodEndInstant == null) {
            periodEndInstant = parseInstantOrNull(attrs.getEnds_at());
        }
        LocalDateTime membershipPeriodEnd = periodEndInstant != null
                ? LocalDateTime.ofInstant(periodEndInstant, ZoneOffset.UTC)
                : LocalDateTime.now().plusMonths(1);

        Instant createdAtInstant = parseInstantOrNull(attrs.getCreated_at());
        LocalDate membershipStartDate = createdAtInstant != null
                ? LocalDateTime.ofInstant(createdAtInstant, ZoneOffset.UTC).toLocalDate()
                : LocalDate.now();

        String customerId = attrs.getCustomer_id() != null ? String.valueOf(attrs.getCustomer_id()) : null;
        String variantId = attrs.getVariant_id() != null ? String.valueOf(attrs.getVariant_id()) : null;
        String subId = dto.getData().getId();
        String newStatus = attrs.getStatus() != null ? attrs.getStatus().toUpperCase() : "UNKNOWN";

        Membership membership = membershipRepository.findByUser(user)
                .stream().findFirst()
                .orElseGet(() -> {
                    System.out.println("⚠️ 멤버십이 없어서 새로 생성합니다.");
                    Membership newMembership = new Membership();
                    newMembership.setUser(user);
                    newMembership.setMembershipStatus("PENDING");
                    newMembership.setMembershipCustomerId(customerId);
                    newMembership.setMembershipSubId(subId);
                    newMembership.setMembershipVariantId(variantId);
                    newMembership.setMembershipPeriodEnd(membershipPeriodEnd);
                    newMembership.setMembershipStartDate(membershipStartDate);
                    return membershipRepository.save(newMembership);
                });

        String oldStatus = membership.getMembershipStatus();
        membership.setMembershipStatus(newStatus);
        membership.setMembershipCustomerId(customerId);
        membership.setMembershipSubId(subId);
        membership.setMembershipVariantId(variantId);
        membership.setMembershipPeriodEnd(membershipPeriodEnd);
        membershipRepository.save(membership);

        historyRepository.save(MembershipHistory.builder()
                .membership(membership)
                .actionType(dto.getMeta().getEvent_name())
                .previousStatus(oldStatus)
                .newStatus(newStatus)
                .historyNote("구독 상태 변경: " + oldStatus + " → " + newStatus)
                .build());

        // ── 마이페이지 등급(User.membership) 동기화 + 가입 쿠폰 발급 ──
        if ("ACTIVE".equals(newStatus) && !"premium".equals(user.getMembership())) {
            user.setMembership("premium");
            user.setMembershipStart(LocalDate.now());
            user.setMembershipEnd(membershipPeriodEnd.toLocalDate());
            userRepository.save(user);
            couponService.issuePremiumSignupCouponsIfNeeded(user);
        } else if ("EXPIRED".equals(newStatus) && "premium".equals(user.getMembership())) {
            user.setMembership("basic");
            user.setMembershipEnd(LocalDate.now());
            userRepository.save(user);
        }
        System.out.println("🔗 urls: " + attrs.getUrls());
    }

    public RefundEligibilityDto checkRefundEligibility(User user) {
        Membership membership = membershipRepository.findByUser(user)
                .stream().findFirst().orElse(null);

        if (membership == null || !List.of("ACTIVE", "CANCELLED").contains(membership.getMembershipStatus())) {
            return new RefundEligibilityDto(false, "현재 활성화된 멤버십이 없습니다.");
        }

        MembershipPayments latestPayment = paymentRepository
                .findByMembershipAndPaymentStatusOrderByMembershipHistoryDateDesc(membership, "PAID")
                .stream().findFirst().orElse(null);

        if (latestPayment == null) {
            return new RefundEligibilityDto(false, "결제 내역을 찾을 수 없습니다.");
        }

        LocalDateTime paidAt = latestPayment.getMembershipHistoryDate();
        if (paidAt.isBefore(LocalDateTime.now().minusDays(7))) {
            return new RefundEligibilityDto(false, "결제일로부터 7일이 지나 환불 신청이 불가능합니다.");
        }

        boolean usedBenefit = payCheckRepository.existsPaidReservationByUserSince(user, paidAt);
        if (usedBenefit) {
            return new RefundEligibilityDto(false, "이미 예매에 사용하여 환불이 불가능합니다.");
        }

        return new RefundEligibilityDto(true, "환불 가능합니다.");
    }

    public RefundEligibilityDto processRefund(User user) {
        RefundEligibilityDto eligibility = checkRefundEligibility(user);
        if (!eligibility.isEligible()) {
            return eligibility;
        }

        Membership membership = membershipRepository.findByUser(user).stream().findFirst().orElseThrow();
        MembershipPayments latestPayment = paymentRepository
                .findByMembershipAndPaymentStatusOrderByMembershipHistoryDateDesc(membership, "PAID")
                .stream().findFirst().orElseThrow();

        String invoiceId = latestPayment.getMembershipInvoiceId();
        if (invoiceId == null) {
            return new RefundEligibilityDto(false, "환불 처리에 필요한 결제 정보가 없습니다. 고객센터에 문의해주세요.");
        }

        try {
            lemonSqueezyRefundService.refundSubscriptionInvoice(invoiceId, null);
        } catch (Exception e) {
            System.out.println("❌ Lemon Squeezy 환불 API 호출 실패: " + e.getMessage());
            return new RefundEligibilityDto(false, "환불 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        String previousStatus = membership.getMembershipStatus();
        membership.setMembershipStatus("REFUNDED");
        membershipRepository.save(membership);

        latestPayment.setPaymentStatus("REFUNDED");
        paymentRepository.save(latestPayment);

        user.setMembership("basic");
        user.setMembershipStart(null);
        user.setMembershipEnd(null);
        userRepository.save(user);

        userCouponRepository.findByUserAndUserCouponStatus(user, 0).stream()
                .filter(uc -> !"신규가입 웰컴 쿠폰".equals(uc.getCoupon().getCouponName()))
                .forEach(uc -> {
                    uc.setUserCouponStatus(2);
                    userCouponRepository.save(uc);
                });

        historyRepository.save(MembershipHistory.builder()
                .membership(membership)
                .actionType("REFUND")
                .previousStatus(previousStatus)
                .newStatus("REFUNDED")
                .historyNote("사용자 환불 신청 승인")
                .build());

        return new RefundEligibilityDto(true, "환불이 완료되었습니다.");


    }

    @Transactional
    public RefundEligibilityDto cancel(User user) {
        Membership membership = membershipRepository.findByUser(user).stream().findFirst().orElse(null);

        if (membership == null || !"ACTIVE".equals(membership.getMembershipStatus())) {
            return new RefundEligibilityDto(false, "현재 활성화된 멤버십이 없습니다.");
        }

        String subId = membership.getMembershipSubId();
        if (subId == null) {
            return new RefundEligibilityDto(false, "구독 정보를 찾을 수 없습니다. 고객센터에 문의해주세요.");
        }

        try {
            lemonSqueezyRefundService.cancelSubscription(subId);

            membership.setMembershipStatus("CANCELLED");
            membershipRepository.save(membership);
            historyRepository.save(MembershipHistory.builder()
                    .membership(membership)
                    .actionType("CANCEL_REQUEST")
                    .previousStatus("ACTIVE")
                    .newStatus("CANCELLED")
                    .historyNote("사용자 직접 해지 신청 (이번 달 혜택 유지)")
                    .build());

        } catch (Exception e) {
            System.out.println("❌ Lemon Squeezy 구독 취소 API 호출 실패: " + e.getMessage());
            return new RefundEligibilityDto(false, "해지 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        return new RefundEligibilityDto(true, "해지 신청이 완료되었습니다. 이번 결제 기간이 끝나는 날까지는 계속 이용하실 수 있습니다.");
    }

    // ── 결제 관련 이벤트 처리 ──
    private void handlePaymentEvent(User user, WebhookRequestDto dto, WebhookRequestDto.WebhookAttributes attrs, String paymentStatus) {
        Membership membership = membershipRepository.findByUser(user)
                .stream().findFirst()
                .orElse(null);

        if (membership == null) {
            System.out.println("⚠️ 결제 이벤트가 왔지만 연결된 멤버십이 없어서 건너뜁니다. (user: " + user.getUserEmail() + ")");
            return;
        }

        if (attrs.getTotal() == null) {
            System.out.println("ℹ️ 금액(total) 정보가 없는 이벤트라 결제 내역은 건너뜁니다.");
            return;
        }

        String orderRef;
        if (attrs.getOrder_id() != null) {
            orderRef = String.valueOf(attrs.getOrder_id());
        } else if (attrs.getOrder_number() != null) {
            orderRef = String.valueOf(attrs.getOrder_number());
        } else {
            orderRef = dto.getData().getId();
        }

        String cardBrand = attrs.getCard_brand() != null ? attrs.getCard_brand() : "UNKNOWN";
        String cardLastFour = attrs.getCard_last_four() != null ? attrs.getCard_last_four() : "0000";

        MembershipPayments payment = MembershipPayments.builder()
                .membership(membership)
                .membershipOrderId(orderRef)
                .membershipPayAmount(attrs.getTotal() / 100)
                .paymentStatus(paymentStatus)
                .cardBrand(cardBrand)
                .cardLastFour(cardLastFour)
                .membershipHistoryDate(LocalDateTime.now())
                .membershipInvoiceId(dto.getData().getId())
                .build();
        paymentRepository.save(payment);

        if ("PAID".equals(paymentStatus)) {
            couponService.renewPremiumCouponsOnPayment(user);
        }
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            System.err.println("⚠️ 날짜 파싱 실패: " + value);
            return null;
        }
    }

    @Transactional
    public void resume(User user) {
        Membership membership = membershipRepository.findByUser(user).stream()
                .max(java.util.Comparator.comparing(Membership::getMembershipId))
                .orElseThrow(() -> new IllegalArgumentException("멤버십 정보가 없습니다."));

        if ("ACTIVE".equals(membership.getMembershipStatus())) {
            throw new IllegalStateException("이미 활성화된 멤버십입니다.");
        }

        try {
            lemonSqueezyRefundService.resumeSubscription(membership.getMembershipSubId());

            membership.setMembershipStatus("ACTIVE");
            membershipRepository.save(membership);

        } catch (Exception e) {
            throw new RuntimeException("구독 재개 처리 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

}
