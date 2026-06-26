package com.ticketflow.dto;

import com.ticketflow.entity.Concert;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ConcertResponseDto {
    private final String concertId;
    private final String concertName;
    private final String posterUrl;
    private final LocalDate startDate; // LocalDate로 수정
    private final LocalDate endDate;   // LocalDate로 수정
    private final String hallName;

    public ConcertResponseDto(Concert c) {
        this.concertId = c.getConcertId();
        this.concertName = c.getConcertName();
        this.posterUrl = c.getConcertPosterUrl();
        this.startDate = c.getConcertStartDate();
        this.endDate = c.getConcertEndDate();
        // NullPointerException 방지를 위해 엔티티에서 가져올 때 안전하게 처리
        this.hallName = (c.getHall() != null) ? c.getHall().getHallName() : "미정";.
    }
}