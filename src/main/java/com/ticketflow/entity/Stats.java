package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stats")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_id")
    private Long statsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "male_ratio", nullable = false)
    @Builder.Default
    private Float maleRatio = 0.0f;

    @Column(name = "female_ratio", nullable = false)
    @Builder.Default
    private Float femaleRatio = 0.0f;

    @Column(name = "age_10s_ratio", nullable = false)
    @Builder.Default
    private Float age10sRatio = 0.0f;

    @Column(name = "age_20s_ratio", nullable = false)
    @Builder.Default
    private Float age20sRatio = 0.0f;

    @Column(name = "age_30s_ratio", nullable = false)
    @Builder.Default
    private Float age30sRatio = 0.0f;

    @Column(name = "age_40s_ratio", nullable = false)
    @Builder.Default
    private Float age40sRatio = 0.0f;

    @Column(name = "age_50s_ratio", nullable = false)
    @Builder.Default
    private Float age50sRatio = 0.0f;

    @Column(name = "reservation_rate", nullable = false)
    @Builder.Default
    private Float reservationRate = 0.0f;

    @Column(name = "predict_sold_out_rate", nullable = false)
    @Builder.Default
    private Float predictSoldOutRate = 0.0f;

    @Column(name = "total_reservations")
    @Builder.Default
    private Long totalReservations = 0L;

    @UpdateTimestamp
    @Column(name = "stats_updated_at")
    private LocalDateTime statsUpdatedAt;
}
