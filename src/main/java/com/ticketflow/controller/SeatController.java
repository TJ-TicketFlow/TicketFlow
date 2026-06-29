package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;

import com.ticketflow.entity.SelectedSeat;
import com.ticketflow.entity.User;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.service.ConcertService;
import com.ticketflow.service.SeatService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // 💡 트랜잭션 추가

// 💡 DTO 클래스명을 올바른 대문자 기반의 BookingPrepareRequest로 교체합니다!
import com.ticketflow.dto.selectedSeat;
import com.ticketflow.repository.SelectedSeatRepository;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {

    private final SeatService seatService;
    private final ConcertService concertService;
    private final UserRepository userRepository;
    private final SelectedSeatRepository selectedSeatRepository;
    private final UserService userService;


    private final SeatRepository seatRepository;

    @PostMapping("/api/booking/prepare")
    public ResponseEntity<?> prepareBooking(
            @RequestBody Map<String, Object> bookingData,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        System.out.println("====== ✈️ [백엔드] 프론트엔드 예매 데이터 수신 ======");
        System.out.println("데이터 확인: " + bookingData);

        // 1. 현재 로그인한 유저의 정보 가져오기
        // 로그인 안 된 경우
        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAIL");
            response.put("message", "로그인이 필요합니다.");

            return ResponseEntity.status(401).body(response);
        }

        User user = userService.findByUserId(userDetails.getUsername());
        Long userNo = user.getUserNo();

        try {
            // 2. 서비스 로직을 태워서 DB에 저장하고, 진짜 영수증 번호 받아오기
            Long realReservationKey = seatService.processBookingAndGetReservationKey(bookingData, userNo);

            // 3. 발급받은 번호를 프론트엔드에 다시 던져줍니다.
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("reservationKey", realReservationKey);

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
    @ResponseBody
    @GetMapping("/api/concert/{concertId}") // 🎯 프론트가 요청한 /seat/api/concert/{공연ID}와 매핑됨
    public ResponseEntity<?> getConcertInfo(@PathVariable String concertId) {
        try {
            System.out.println("====== [백엔드] 공연 상세 정보 및 레이아웃 조회 요청: " + concertId);

            // 1. 이미 컨트롤러 상단에 주입되어 있는 concertService를 사용하여 공연 엔티티(DB 데이터)를 가져옵니다.
            Concert concert = concertService.findById(concertId);

            // 2. seatService를 통해 레이아웃 타입도 함께 가져옵니다.
            String layoutType = seatService.getSeatLayoutType(concertId);

            if (concert == null) {
                return ResponseEntity.status(404).body(Map.of("message", "해당 공연 정보를 찾을 수 없습니다."));
            }

            // 3. 자바스크립트가 원하는 필드명 그대로 Map에 꽉꽉 채워서 넘겨줍니다.
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("concertId", concertId);
            responseData.put("layoutType", layoutType);

            // 💡 엔티티 내부의 Getter 명칭(getConcertName 등)에 맞게 수집하여 담아줍니다.
            responseData.put("concertName", concert.getConcertName());
            responseData.put("concertPosterUrl", concert.getConcertPosterUrl());
            responseData.put("concertDate", concert.getConcertStartDate());
            responseData.put("concertRuntime", concert.getConcertRuntime()); // 또는 getConcertTime()
            responseData.put("concertPriceInfo", concert.getConcertPriceInfo());

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            e.printStackTrace(); // 인텔리제이 콘솔에 에러 출력
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/seats/{concertId}") // 🎯 자바스크립트의 /seat/api/seats/{공연ID} 요청을 처리합니다.
    public ResponseEntity<List<Seat>> getSeatList(@PathVariable String concertId) {
        System.out.println("====== 💺 [백엔드] 좌석 리스트 조회 요청 (공연 ID): " + concertId);

        // 이미 SeatService에 완벽하게 만들어져 있는 getSeats 메서드를 호출합니다.
        List<Seat> seats = seatService.getSeats(concertId);

        return ResponseEntity.ok(seats);
    }

    @ResponseBody
    @GetMapping("/layout/{concertId}")
    public ResponseEntity<String> getSeatLayoutType(@PathVariable String concertId) {
        return ResponseEntity.ok(seatService.getSeatLayoutType(concertId));
    }

}