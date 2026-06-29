package com.ticketflow.dto;

/**
 * 좌석 1개의 상태 변경을 실시간으로 알리기 위한 메시지.
 * type: "SELECTED"(선점됨) | "CANCELLED"(선점 해제됨)
 */
public record SeatEventMessage(
        String type,
        String seatId,
        Long userNo
) {
}
