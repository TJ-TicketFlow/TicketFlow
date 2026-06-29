package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;
import com.ticketflow.entity.User;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.repository.SelectedSeatRepository;
import com.ticketflow.service.ConcertService;
import com.ticketflow.service.SeatService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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
    private final UserService userService;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final SelectedSeatRepository selectedSeatRepository;

    /**
     * 1. 좌석 선택 메인 페이지 반환 (Thymeleaf 뷰)
     * GET /seat/{concertId}
     */
    @GetMapping("/{concertId}")
    public String showSeatMap(@PathVariable String concertId,
                              HttpServletRequest request,
                              Model model,
                              Principal principal) {

        // Spring Security CSRF 토큰 세팅 (자바스크립트 fetch 통신용)
        org.springframework.security.web.csrf.CsrfToken csrfToken =
                (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());

        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }

        model.addAttribute("concertId", concertId);

        // 실시간 좌석 선점(WebSocket 연동)에 필요한 현재 로그인 사용자의 userNo를 화면에 내려줌
        Long userNo = null;
        if (principal != null) {
            userNo = userRepository.findByUserId(principal.getName())
                    .map(User::getUserNo)
                    .orElse(null);
        }
        model.addAttribute("userNo", userNo);

        // 🎯 결제 페이지 호출 규격 통일 템플릿 경로 반환
        return "concert/seatmap";
    }

    /**
     * 2. 공연 기본 정보 및 레이아웃 타입 조회 API (Ajax 요청용)
     * GET /seat/api/concert/{concertId}
     */
    @ResponseBody
    @GetMapping("/api/concert/{concertId}")
    public ResponseEntity<?> getConcertInfo(@PathVariable String concertId) {
        try {
            System.out.println("====== [백엔드] 공연 상세 정보 및 레이아웃 조회 요청: " + concertId);

            Concert concert = concertService.findById(concertId);
            String layoutType = seatService.getSeatLayoutType(concertId);

            if (concert == null) {
                return ResponseEntity.status(404).body(Map.of("message", "해당 공연 정보를 찾을 수 없습니다."));
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("concertId", concertId);
            responseData.put("layoutType", layoutType);
            responseData.put("concertName", concert.getConcertName());
            responseData.put("concertPosterUrl", concert.getConcertPosterUrl());
            responseData.put("concertDate", concert.getConcertStartDate().toString());
            responseData.put("concertRuntime", concert.getConcertRuntime());
            responseData.put("concertPriceInfo", concert.getConcertPriceInfo());

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 3. 특정 공연의 전체 좌석 배치 목록 조회 API
     * GET /seat/api/seats/{concertId} 또는 GET /seat/api/{concertId} (호환성 유지)
     */
    @ResponseBody
    @GetMapping({"/api/seats/{concertId}", "/api/{concertId}"})
    public ResponseEntity<List<Seat>> getSeatList(@PathVariable String concertId) {
        System.out.println("====== 💺 [백엔드] 좌석 리스트 조회 요청 (공연 ID): " + concertId);
        List<Seat> seats = seatService.getSeats(concertId);
        return ResponseEntity.ok(seats);
    }

    /**
     * 4. 공연별 좌석 배치 레이아웃 구조 코드 단독 조회
     * GET /seat/layout/{concertId}
     */
    @ResponseBody
    @GetMapping("/layout/{concertId}")
    public ResponseEntity<String> getSeatLayoutType(@PathVariable String concertId) {
        return ResponseEntity.ok(seatService.getSeatLayoutType(concertId));
    }

    /**
     * 5. 실시간 웹소켓 기반 좌석 선택 (선점 처리)
     * POST /seat/select
     */
    @ResponseBody
    @PostMapping("/select")
    public ResponseEntity<String> selectSeat(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();
        Long userNo = Long.valueOf(data.get("userNo").toString());

        seatService.selectSeat(seatId, userNo);
        return ResponseEntity.ok("좌석 선택 완료");
    }

    /**
     * 6. 실시간 웹소켓 기반 좌석 취소 (선점 해제)
     * POST /seat/cancel
     */
    @ResponseBody
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelSeat(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();

        seatService.cancelSeat(seatId);
        return ResponseEntity.ok("좌석 취소 완료");
    }

    /**
     * 7. 좌석 상태값 직접 변경 제어 (어드민 / 시스템용)
     * PUT /seat/status
     */
    @ResponseBody
    @PutMapping("/status")
    public ResponseEntity<String> updateSeatStatus(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();
        Short status = Short.valueOf(data.get("status").toString());

        seatService.updateSeatStatus(seatId, status);
        return ResponseEntity.ok("상태 변경 완료");
    }

    /**
     * 8. 특정 공연 등급별 단일 좌석 가격 조회
     * GET /seat/price/{concertId}/{seatClass}
     */
    @ResponseBody
    @GetMapping("/price/{concertId}/{seatClass}")
    public ResponseEntity<Integer> getPrice(@PathVariable String concertId, @PathVariable String seatClass) {
        int price = seatService.calculatePrice(concertId, seatClass);
        return ResponseEntity.ok(price);
    }

    /**
     * 9. 🌟 프론트엔드 좌석 결제 준비 단계 (예매 임시 장부 등록 처리)
     * POST /seat/api/booking/prepare
     */
    @PostMapping("/api/booking/prepare")
    public ResponseEntity<?> prepareBooking(
            @RequestBody Map<String, Object> bookingData,
            @AuthenticationPrincipal UserDetails userDetails) {

        System.out.println("====== ✈️ [백엔드] 프론트엔드 예매 데이터 수신 ======");
        System.out.println("데이터 확인: " + bookingData);

        // 1. 보안 검증 및 로그인 여부 확인
        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAIL");
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.findByUserId(userDetails.getUsername());
        Long userNo = user.getUserNo();

        try {
            // 2. 비즈니스 서비스 로직 작동 (DB 저장 및 고유 고유 가선점 ID 영수증 발급)
            Long realReservationKey = seatService.processBookingAndGetReservationKey(bookingData, userNo);

            // 3. 앞단 자바스크립트와 변수 연동 규격 동기화 (bookingId 명칭으로 매핑)
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("bookingId", realReservationKey); // 🌟 주소창에 들어가는 bookingId 명칭과 일치하도록 세팅

            System.out.println("✅ 예매 임시 장부 저장 완료! 예약 번호: " + realReservationKey);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("🚨 예매 장부 생성 실패: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "FAIL");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}