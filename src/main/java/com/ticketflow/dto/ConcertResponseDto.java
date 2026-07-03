package com.ticketflow.dto;

import com.ticketflow.entity.Concert;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ConcertResponseDto {
    private final String concertId;
    private final String concertName;
    private final String posterUrl;      // 👈 HTML에서 concert.posterUrl 로 접근
    private final LocalDate startDate;    // 👈 HTML에서 concert.startDate 로 접근
    private final LocalDate endDate;      // 👈 HTML에서 concert.endDate 로 접근
    private final String hallName;       // 👈 HTML에서 concert.hallName 로 접근
    private final String concertGenre;   // 💡 [추가] 누락되었던 장르 필드 반영

    private Double predictSoldOutRate;   // AI 예매율 예측값

    public ConcertResponseDto(Concert c) {
        this.concertId = c.getConcertId();
        this.concertName = c.getConcertName();
        this.posterUrl = c.getConcertPosterUrl();
        this.startDate = c.getConcertStartDate();
        this.endDate = c.getConcertEndDate();
        this.concertGenre = c.getConcertGenre(); // 💡 엔티티에서 장르 가져오기
        this.hallName = (c.getHall() != null) ? c.getHall().getHallName() : "미정";
    }

    public void setPredictSoldOutRate(Double predictSoldOutRate) {
        this.predictSoldOutRate = predictSoldOutRate;
    }
}