package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPayments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "membership_order_id", length = 100, nullable = false)
    private String membershipOrderId;

    @Column(name = "membership_pay_amount", nullable = false)
    private Integer membershipPayAmount;

    @Column(name = "payment_status", length = 20, nullable = false)
    private String paymentStatus;

    @Column(name = "card_brand", length = 20, nullable = false)
    private String cardBrand;

    @Column(name = "card_last_four", length = 4, nullable = false)
    private String cardLastFour;

    @Column(name = "membership_history_date", nullable = false)
    private LocalDateTime membershipHistoryDate;

    @Column(name = "webhook_event_id", length = 100, unique = true)
    private String webhookEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;
}
