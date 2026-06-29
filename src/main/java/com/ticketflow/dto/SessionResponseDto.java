package com.ticketflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponseDto {
    private String time;      // 예: "14:00"
    private boolean soldOut;  // 매진 여부 (true면 버튼 비활성화)
}