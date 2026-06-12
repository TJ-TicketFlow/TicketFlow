package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupon")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_coupon_id")
    private Long userCouponId;

    // 0: 사용가능, 1: 사용완료, 2: 기간만료
    @Column(name = "user_coupon_status", nullable = false)
    @Builder.Default
    private Integer userCouponStatus = 0;

    @CreationTimestamp
    @Column(name = "user_coupon_issued_at", updatable = false, nullable = false)
    private LocalDateTime userCouponIssuedAt;

    @Column(name = "user_coupon_expire_at", nullable = false)
    private LocalDateTime userCouponExpireAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;
}
