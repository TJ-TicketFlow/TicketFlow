package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Long userNo;

    @Column(name = "user_id", length = 50, unique = true, nullable = false)
    private String userId;

    @Column(name = "user_pw", length = 255, nullable = false)
    private String userPw;

    @Column(name = "user_email", length = 50, unique = true, nullable = false)
    private String userEmail;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userName;

    @Column(name = "user_birth")
    private LocalDate userBirth;

    @Column(name = "user_address", length = 255)
    private String userAddress;

    @Column(name = "user_phone_number", length = 50, nullable = false)
    private String userPhoneNumber;

    // 0: 여성, 1: 남성, null: 선택안함
    @Column(name = "user_sex")
    private Integer userSex;

    // 멤버십 등급: "premium", "basic", null
    @Column(name = "membership", length = 50)
    @Builder.Default
    private String membership = "basic";

    // 멤버십 시작일
    @Column(name = "membership_start")
    private LocalDate membershipStart;

    // 멤버십 종료일
    @Column(name = "membership_end")
    private LocalDate membershipEnd;

    @CreationTimestamp
    @Column(name = "user_created_at", updatable = false)
    private LocalDateTime userCreatedAt;

    @UpdateTimestamp
    @Column(name = "user_updated_at")
    private LocalDateTime userUpdatedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserCoupon> userCoupons = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Membership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SelectedSeat> selectedSeats = new ArrayList<>();
}