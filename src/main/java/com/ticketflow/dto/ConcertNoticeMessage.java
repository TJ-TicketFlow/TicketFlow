package com.ticketflow.dto;

/**
 * 공연 단위의 공지성 실시간 알림.
 * type: "SOLD_OUT"(전 좌석 마감) 등으로 확장 가능
 */
public record ConcertNoticeMessage(
        String type,
        String message,
        long remainingSeats
) {
}
