package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservation")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_key")
    private Long reservationKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_seat_id", nullable = false)
    private SelectedSeat selectedSeat;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_count", nullable = false)
    private Integer reservationCount;

    @CreationTimestamp
    @Column(name = "reservation_created_at", updatable = false)
    private LocalDateTime reservationCreatedAt;

    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pay> pays = new ArrayList<>();
}
