package com.ticketflow.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class selectedSeat {
    private String concertId;
    private String ticketType;
    private int totalPrice;
    private List<String> selectedSeats;               // 지정석 전용 배열 매칭용
    private Map<String, Integer> quantities;          // 스탠딩 전용 수량 맵 매칭용
}