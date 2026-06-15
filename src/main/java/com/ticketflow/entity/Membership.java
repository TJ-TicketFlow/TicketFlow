package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Long membershipId;

    @Column(name = "membership_customer_id", length = 100, nullable = false)
    private String membershipCustomerId;

    @Column(name = "membership_sub_id", length = 100, nullable = false)
    private String membershipSubId;

    @Column(name = "membership_variant_id", length = 100, nullable = false)
    private String membershipVariantId;

    @Column(name = "membership_status", length = 20, nullable = false)
    private String membershipStatus;

    @Column(name = "membership_period_end", nullable = false)
    private LocalDateTime membershipPeriodEnd;

    @Column(name = "membership_start_date", nullable = false)
    private LocalDate membershipStartDate;

    @CreationTimestamp
    @Column(name = "membership_created_at", updatable = false, nullable = false)
    private LocalDateTime membershipCreatedAt;

    @UpdateTimestamp
    @Column(name = "membership_updated_at", nullable = false)
    private LocalDateTime membershipUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;
}
