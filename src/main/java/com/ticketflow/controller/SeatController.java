package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;
import com.ticketflow.service.ConcertService;
import com.ticketflow.service.SeatService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {

    private final SeatService seatService;
    private final ConcertService concertService;
    private final UserService userService;


    @GetMapping("/{concertId}")
    public String seatPage(@PathVariable String concertId, Model model) {
        model.addAttribute("concertId", concertId);
        return "concert/seatmap"; //나중에 검토
    }
    /**
     * 공연 기본 정보 조회 (이름, 포스터, 시간, 날짜)
     * GET /seat/api/concert/{concertId}
     */
    @GetMapping("/api/concert/{concertId}")
    public ResponseEntity<?> getConcertInfo(@PathVariable String concertId) {
        Concert concert = concertService.findById(concertId);

        // 엔티티를 통째로 주지 않고 가벼운 Map에 담아 무한 참조를 원천 차단합니다.
        Map<String, Object> response = new HashMap<>();
        response.put("concertName", concert.getConcertName());
        response.put("concertPosterUrl", concert.getConcertPosterUrl());
        response.put("concertTime", concert.getConcertRuntime());
        response.put("concertDate", concert.getConcertStartDate().toString());
        response.put("concertPriceInfo", concert.getConcertPriceInfo());

        return ResponseEntity.ok(response);
    }

    /**
     * 1. 특정 공연의 전체 좌석 조회
     * GET /seat/api/{concertId}
     */
    @GetMapping("/api/{concertId}")
    public ResponseEntity<List<Seat>> getSeats(@PathVariable String concertId) {
        List<Seat> seats = seatService.getSeats(concertId);
        return ResponseEntity.ok(seats);
    }

    /**
     * 공연별 좌석 배치 타입 조회 (SEAT_A, SEAT_B 등)
     * GET /seat/layout/{concertId}
     */
    @GetMapping("/layout/{concertId}")
    public ResponseEntity<String> getLayout(@PathVariable String concertId) {
      
        return ResponseEntity.ok(seatService.getSeatLayoutType(concertId));
    }

    /**
     * 2. 좌석 선택 (선점)
     * POST /seat/select
     * body: {"seatId": "A1", "userNo": 1}
     */
    @PostMapping("/select")
    public ResponseEntity<String> selectSeat(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();
        Long userNo = Long.valueOf(data.get("userNo").toString());

        seatService.selectSeat(seatId, userNo);
        return ResponseEntity.ok("좌석 선택 완료");
    }

    /**
     * 3. 좌석 취소
     * POST /seat/cancel
     * body: {"seatId": "A1"}
     */
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelSeat(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();

        seatService.cancelSeat(seatId);
        return ResponseEntity.ok("좌석 취소 완료");
    }

    /**
     * 4. 좌석 예매 상태 강제 변경
     * PUT /seat/status
     * body: {"seatId": "A1", "status": 0}
     */
    @PutMapping("/status")
    public ResponseEntity<String> updateSeatStatus(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();
        Short status = Short.valueOf(data.get("status").toString());

        seatService.updateSeatStatus(seatId, status);
        return ResponseEntity.ok("상태 변경 완료");
    }

    /**
     * 5. 특정 등급의 단일 좌석 가격 조회
     * GET /seat/price/{concertId}/{seatClass}
     */
    @GetMapping("/price/{concertId}/{seatClass}")
    public ResponseEntity<Integer> getPrice(
            @PathVariable String concertId,
            @PathVariable String seatClass
    ) {
        int price = seatService.calculatePrice(concertId, seatClass);
        return ResponseEntity.ok(price);
    }

    // 🌟 이 부분을 기존 prepareBooking 대신 완전히 덮어씌워 주세요!
    @PostMapping("/api/booking/prepare")
    public ResponseEntity<?> prepareBooking(
            @RequestBody Map<String, Object> bookingData,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        System.out.println("====== ✈️ [백엔드] 프론트엔드 예매 데이터 수신 ======");
        System.out.println("데이터 확인: " + bookingData);

        // 1. 현재 로그인한 유저의 정보 가져오기
        Long userNo = 1L; // 기본값 (로그인 연동 전 에러 방지용)
        if (userDetails != null) {
            com.ticketflow.entity.User user = userService.findByUserId(userDetails.getUsername());
            userNo = user.getUserNo();
        }

        try {
            // 2. 서비스 로직을 태워서 DB에 저장하고, 찐 영수증 번호 받아오기!
            Long realReservationKey = seatService.processBookingAndGetReservationKey(bookingData, userNo);

            // 3. 발급받은 번호를 프론트엔드에 다시 던져줍니다.
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("reservationKey", realReservationKey); // 🌟 여기가 핵심!

            System.out.println("✅ 예매 장부 생성 완료! 예약 번호: " + realReservationKey);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("🚨 예매 장부 생성 실패: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "FAIL");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} // 클래스 마지막 닫는 괄호


