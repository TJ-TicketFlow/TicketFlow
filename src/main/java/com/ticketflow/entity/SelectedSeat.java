package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "selected_seat")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "selected_seat_id")
    private Long selectedSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // 0: 미선택, 1: 선택/결제중, 2: 선택완료
    @Column(name = "seat_state", nullable = false)
    @Builder.Default
    private Short seatState = 0;

    @Column(name = "price", nullable = false)
    private Long price;

    @OneToMany(mappedBy = "selectedSeat", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
