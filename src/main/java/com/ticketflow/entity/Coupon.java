package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupon")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_name", length = 255, nullable = false)
    private String couponName;

    @Column(name = "coupon_discount_rate", nullable = false)
    private Integer couponDiscountRate;

    @Column(name = "coupon_valid_days", nullable = false)
    private Integer couponValidDays;
}
