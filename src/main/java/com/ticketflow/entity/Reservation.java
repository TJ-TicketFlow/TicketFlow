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

    // 추가 변경 사항 회차 시간 추가
    @Column(name = "session_time", length = 20, nullable = false)
    private String sessionTime; // 예: "14:00"

    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pay> pays = new ArrayList<>();

    // 화면 출력용 예쁜 글자
    @Column(name = "selected_seats_text", length = 500)
    private String selectedSeatsText;

    // 🌟 [추가] 취소할 때 백엔드가 읽어들일 진짜 DB 아이디 리스트!
    @Column(name = "reserved_seat_ids", length = 500)
    private String reservedSeatIds;
}