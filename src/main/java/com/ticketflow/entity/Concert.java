package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concert")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concert {

    @Id
    @Column(name = "concert_id", length = 50, nullable = false)
    private String concertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "concert_name", length = 50, nullable = false)
    private String concertName;

    @Column(name = "concert_start_date", nullable = false)
    private LocalDate concertStartDate;

    @Column(name = "concert_end_date", nullable = false)
    private LocalDate concertEndDate;

    @Column(name = "concert_cast", length = 1000)
    private String concertCast;

    @Column(name = "concert_runtime", length = 1000)
    private String concertRuntime;

    @Column(name = "concert_age_limit", length = 1000)
    private String concertAgeLimit;

    @Column(name = "concert_producer", length = 1000)
    private String concertProducer;

    @Column(name = "concert_price_info", length = 1000)
    private String concertPriceInfo;

    @Column(name = "concert_poster_url", length = 1000)
    private String concertPosterUrl;

    @Column(name = "concert_status", length = 50, nullable = false)
    private String concertStatus;

    @Column(name = "concert_time", length = 1000)
    private String concertTime;

    @Column(name = "concert_info_images", length = 1000)
    private String concertInfoImages;

    @Column(name = "concert_genre", length = 50)
    private String concertGenre;

    @Column(name = "concert_seat_scale")
    private Integer concertSeatScale;

    @Column(name = "concert_booking_count", nullable = false)
    @Builder.Default
    private Integer concertBookingCount = 0;

    @Column(name = "concert_view_count", nullable = false)
    @Builder.Default
    private Integer concertViewCount = 0;

    @Column(name = "concert_wishlist_count", nullable = false)
    @Builder.Default
    private Integer concertWishlistCount = 0;

    @OneToMany(mappedBy = "concert", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wishlist> wishlists = new ArrayList<>();

    @OneToMany(mappedBy = "concert", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Stats> stats = new ArrayList<>();

    @OneToMany(mappedBy = "concert", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
