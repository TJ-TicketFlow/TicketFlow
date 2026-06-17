package com.ticketflow.dto;

import com.ticketflow.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConcertRankDto {
    private Concert concert;
    private int ranking;
}