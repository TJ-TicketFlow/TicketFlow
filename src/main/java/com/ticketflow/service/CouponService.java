package com.ticketflow.service;

import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final MembershipRepository membershipRepository;

    public void issuePremiumSignupCouponsIfNeeded(User user) {
        Coupon checkCoupon = couponRepository.findByCouponName("프리미엄 가입 쿠폰")
                .orElseThrow(() -> new IllegalStateException("쿠폰 마스터를 찾을 수 없음: 프리미엄 가입 쿠폰"));

        if (userCouponRepository.existsByUserAndCouponAndUserCouponStatus(user, checkCoupon, 0)) {
            System.out.println("ℹ️ 이미 사용 가능한 프리미엄 가입 쿠폰이 있는 유저라 건너뜁니다. (user: " + user.getUserEmail() + ")");
            return;
        }

        issueCoupon(user, "프리미엄 가입 쿠폰");
        issueCoupon(user, "프리미엄 가입 쿠폰");
        issueCoupon(user, "프리미엄 가입 스페셜 쿠폰");
        System.out.println("🎁 프리미엄 가입 쿠폰 3장 발급 완료! (user: " + user.getUserEmail() + ")");
    }

    public void issueLoyaltyCouponIfNeeded(Membership membership) {
        User user = membership.getUser();
        Coupon loyaltyCoupon = couponRepository.findByCouponName("프리미엄 3개월 유지 쿠폰")
                .orElseThrow(() -> new IllegalStateException("쿠폰 마스터를 찾을 수 없음: 프리미엄 3개월 유지 쿠폰"));

        if (userCouponRepository.existsByUserAndCoupon(user, loyaltyCoupon)) {
            return;
        }

        issueCoupon(user, "프리미엄 3개월 유지 쿠폰");
        System.out.println("🎁 3개월 유지 보상 쿠폰 발급! (user: " + user.getUserEmail() + ")");
    }

    public void issueCoupon(User user, String couponName) {
        Coupon coupon = couponRepository.findByCouponName(couponName)
                .orElseThrow(() -> new IllegalStateException("쿠폰 마스터를 찾을 수 없음: " + couponName));

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .userCouponStatus(0)
                .userCouponExpireAt(LocalDateTime.now().plusDays(coupon.getCouponValidDays()))
                .build();
        userCouponRepository.save(userCoupon);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void expireOldCoupons() {
        List<UserCoupon> expiredCoupons = userCouponRepository
                .findByUserCouponStatusAndUserCouponExpireAtBefore(0, LocalDateTime.now());

        for (UserCoupon coupon : expiredCoupons) {
            coupon.setUserCouponStatus(2);
            userCouponRepository.save(coupon);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void issueThreeMonthLoyaltyCoupons() {
        List<Membership> activeMemberships = membershipRepository.findByMembershipStatus("ACTIVE");
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

        for (Membership membership : activeMemberships) {
            if (!membership.getMembershipStartDate().isAfter(threeMonthsAgo)) {
                issueLoyaltyCouponIfNeeded(membership);
            }
        }
    }
}