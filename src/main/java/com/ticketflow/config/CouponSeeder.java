package com.ticketflow.config;

import com.ticketflow.entity.Coupon;
import com.ticketflow.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponSeeder implements ApplicationRunner {

    private final CouponRepository couponRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("신규가입 웰컴 쿠폰", 5, 30);
        seedIfMissing("프리미엄 가입 쿠폰", 10, 30);
        seedIfMissing("프리미엄 가입 스페셜 쿠폰", 15, 30);
        seedIfMissing("프리미엄 3개월 유지 쿠폰", 20, 30);
    }

    private void seedIfMissing(String name, int discountRate, int validDays) {
        if (couponRepository.findByCouponName(name).isEmpty()) {
            couponRepository.save(Coupon.builder()
                    .couponName(name)
                    .couponDiscountRate(discountRate)
                    .couponValidDays(validDays)
                    .build());
            System.out.println("🎫 쿠폰 마스터 자동 등록: " + name);
        }
    }
}