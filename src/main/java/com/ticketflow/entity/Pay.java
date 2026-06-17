package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pay")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_no")
    private Long payNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id")
    private UserCoupon userCoupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_key")
    private Reservation reservation;

    @Column(name = "merchant_uid", length = 36, unique = true, nullable = false)
    @Builder.Default
    private String merchantUid = UUID.randomUUID().toString();

    @Column(name = "pay_name", length = 255, nullable = false)
    private String payName;

    @Column(name = "pay_amount")
    private Long payAmount;

    @Column(name = "pay_method", length = 50)
    private String payMethod;

    @Column(name = "buyer_name", length = 50)
    private String buyerName;

    @Column(name = "buyer_email", length = 50)
    private String buyerEmail;

    @Column(name = "pay_del_name", length = 50)
    private String payDelName;

    @Column(name = "pay_del_call", length = 50)
    private String payDelCall;

    @Column(name = "pay_del_postcode", length = 5)
    private String payDelPostcode;

    @Column(name = "pay_del_addr", length = 255)
    private String payDelAddr;

    @Column(name = "pay_status", length = 20, nullable = false)
    @Builder.Default
    private String payStatus = "READY";

    @CreationTimestamp
    @Column(name = "pay_created_at", updatable = false)
    private LocalDateTime payCreatedAt;

    @UpdateTimestamp
    @Column(name = "pay_updated_at")
    private LocalDateTime payUpdatedAt;

    @Column(name = "pay_fail_reason", length = 255)
    private String payFailReason;

    @Column(name = "ls_order_id", length = 100)
    private String LsOrderId;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "receipt_url",length = 500)
    private String receiptUrl;

    @Column(name = "ls_customer_id", length = 100)
    private String LsCustomerId;

    @Column(name = "ls_webhook_event_id",unique = true, length = 50)
    private String LsWebhookEventId;
}
