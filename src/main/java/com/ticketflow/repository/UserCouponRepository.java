package com.ticketflow.repository;

import com.ticketflow.entity.Coupon;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    List<UserCoupon> findByUser_UserId(String userId);
    boolean existsByUserAndCoupon(User user, Coupon coupon);
    List<UserCoupon> findByUserCouponStatusAndUserCouponExpireAtBefore(Integer status, LocalDateTime dateTime);
    List<UserCoupon> findByUserAndUserCouponStatus(User user, Integer userCouponStatus);
    boolean existsByUserAndCouponAndUserCouponStatus(User user, Coupon coupon, Integer userCouponStatus);
}
