package com.ticketflow.dto;

import com.ticketflow.entity.Concert;
import lombok.Getter;

@Getter
public class ConcertResponseDto {
    private final String concertId;
    private final String concertName;
    private final String posterUrl;

    public ConcertResponseDto(Concert c) {
        this.concertId = c.getConcertId();
        this.concertName = c.getConcertName();
        this.posterUrl = c.getConcertPosterUrl();
    }
}