package com.ticketflow.service;

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

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPaymentRepository paymentRepository;
    private final MembershipHistoryRepository historyRepository;
    private final UserCouponRepository userCouponRepository;

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
            // 구독 상태 자체가 바뀌는 이벤트들 → attrs.status를 그대로 신뢰해서 멤버십 상태 갱신
            case "subscription_created":
            case "subscription_updated":
            case "subscription_cancelled":
            case "subscription_resumed":
            case "subscription_expired":
            case "subscription_paused":
            case "subscription_unpaused":
                handleSubscriptionStatusEvent(user, dto, attrs);
                break;

            // 결제가 성공/회복된 이벤트 → 결제 내역만 기록 (상태는 위 이벤트들이 따로 갱신해줌)
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

            // Order 이벤트는 구독 결제(subscription_payment_*)와 중복되므로 로그만 남김
            case "order_created":
            case "order_refunded":
                System.out.println("ℹ️ Order 이벤트는 참고용으로만 로그 처리 (구독 결제는 subscription_payment_* 이벤트가 담당): " + eventName);
                break;

            default:
                System.out.println("ℹ️ 처리하지 않는 이벤트: " + eventName);
        }

        processCouponIfPresent(user, dto);

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

        // 카드 정보가 없는 이벤트를 대비한 폴백
        String cardBrand = attrs.getCard_brand() != null ? attrs.getCard_brand() : "UNKNOWN";
        String cardLastFour = attrs.getCard_last_four() != null ? attrs.getCard_last_four() : "0000";

        MembershipPayments payment = MembershipPayments.builder()
                .membership(membership)
                .membershipOrderId(orderRef)
                .membershipPayAmount(attrs.getTotal())
                .paymentStatus(paymentStatus)
                .cardBrand(cardBrand)
                .cardLastFour(cardLastFour)
                .membershipHistoryDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);
    }

    private void processCouponIfPresent(User user, WebhookRequestDto dto) {
        if (dto.getData().getMetadata() == null || dto.getData().getMetadata().get("user_coupon_id") == null) {
            return;
        }
        try {
            Long couponId = Long.valueOf(dto.getData().getMetadata().get("user_coupon_id").toString());
            user.getUserCoupons().stream()
                    .filter(uc -> uc.getCoupon().getCouponId().equals(couponId))
                    .findFirst()
                    .ifPresent(uc -> {
                        uc.setUserCouponStatus(1);
                        userCouponRepository.save(uc);
                    });
        } catch (Exception e) {
            System.err.println("⚠️ 쿠폰 처리 중 에러 발생: " + e.getMessage());
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
}