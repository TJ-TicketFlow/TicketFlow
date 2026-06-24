package com.ticketflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seat")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @Column(name = "seat_id", length = 20)
    private String seatId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "seat_class", length = 20, nullable = false)
    private String seatClass;

    // 1: 사용가능, 0: 사용불가
    @Column(name = "seat_status", nullable = false)
    @Builder.Default
    private Short seatStatus = 1;

    @Column(name = "seat_row", length = 50, nullable = false)
    private String seatRow;

    @Column(name = "seat_col", length = 50, nullable = false)
    private String seatCol;

    @JsonIgnore
    @OneToMany(mappedBy = "seat", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SelectedSeat> selectedSeats = new ArrayList<>();
}
