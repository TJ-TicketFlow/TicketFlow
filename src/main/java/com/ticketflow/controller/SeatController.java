package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;

import com.ticketflow.entity.SelectedSeat;
import com.ticketflow.service.ConcertService;
import com.ticketflow.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // 💡 트랜잭션 추가

// 💡 DTO 클래스명을 올바른 대문자 기반의 BookingPrepareRequest로 교체합니다!
import com.ticketflow.dto.selectedSeat;
import com.ticketflow.repository.SelectedSeatRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {

    private final SeatService seatService;
    private final ConcertService concertService;
    private final SelectedSeatRepository selectedSeatRepository;

    // ... 중간 @GetMapping, @PostMapping 메서드들은 기존과 동일하므로 생략 ...

    /**
     * 6. 프론트엔드 좌석 결제 준비 및 selected_seat 데이터 등록 처리
     * POST /seat/api/booking/prepare
     */
    /**
     * 6. 프론트엔드 좌석 결제 준비 및 selected_seat 데이터 등록 처리
     * POST /seat/api/booking/prepare
     */
    /**
     * 6. 프론트엔드 좌석 결제 준비 및 selected_seat 데이터 등록 처리
     * POST /seat/api/booking/prepare
     */

    @ResponseBody
    @PostMapping("/api/booking/prepare")
    @Transactional
    public ResponseEntity<?> prepareBooking(@RequestBody com.ticketflow.dto.selectedSeat request) { // 💡 수정 완료!

        System.out.println("====== ✈️ [백엔드] 프론트엔드 예매 데이터 수신 (DTO 변환 완료) ======");
        System.out.println("공연 ID (concertId): " + request.getConcertId());
        System.out.println("티켓팅 타입 (ticketType): " + request.getTicketType());
        System.out.println("총 금액 (totalPrice): " + request.getTotalPrice());

        // 💡 SeatController 내부의 해당 파트를 이렇게 수정하세요!
        if ("SEAT".equals(request.getTicketType())) {
            List<String> seats = request.getSelectedSeats();

            if (seats == null || seats.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "선택된 좌석 데이터가 누락되었습니다."));
            }

            for (String seatId : seats) {
                // 🎯 [핵심 교정] "SEAT_R1_C1" -> "PF277688" + "_R1_C1" 형태로 문자열을 완벽하게 재조합합니다.
                String cleanSeatId = seatId.replace("SEAT_", "");
                String realDbSeatId = request.getConcertId() + "_" + cleanSeatId; // "PF277688_R1_C1" 완성!

                System.out.println("➡️ [DB 조회 및 인서트 시도] 변환된 좌석 식별자: " + realDbSeatId);

                Seat seat = seatService.getSeatById(realDbSeatId);
                // 1. 좌석이 이미 다른 사람에게 예약되었는지 검증
                if (seat.getSeatStatus() == 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "이미 선택된 좌석입니다."));
                }

                // 2. 예약이 가능하다면, 해당 좌석의 상태를 '예약중(0)'으로 변경 (선점 조치)
                seat.setSeatStatus((short) 0);

                // 3. 결제 준비를 위해 selected_seat 테이블에 이 좌석 정보를 매핑하여 저장
                SelectedSeat selectedSeat = new SelectedSeat();
                selectedSeat.setSeat(seat); // 찾은 좌석 객체를 넣어줌
                selectedSeatRepository.save(selectedSeat);
            }
        }
        // ... 이하 생략 ...
        else if ("STANDING".equals(request.getTicketType())) {
            System.out.println("선택된 등급별 수량 (quantities): " + request.getQuantities());
        }
        System.out.println("=========================================================================");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("bookingId", "TEMP_B_" + System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{concertId}")
    public String showSeatMap(@PathVariable String concertId,
                              jakarta.servlet.http.HttpServletRequest request,
                              Model model) {

        org.springframework.security.web.csrf.CsrfToken csrfToken =
                (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());

        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }

        model.addAttribute("concertId", concertId);

        // 🎯 기존 "seatmap" ➡️ "booking/seatmap" 으로 수정!
        return "concert/seatmap";
    }
}