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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1. 좌석 선택 메인 페이지 반환 (Thymeleaf 뷰)
     * GET /seat/{concertId}
     */
    @GetMapping("/{concertId}")
    public String showSeatMap(@PathVariable String concertId,
                              @RequestParam(required = false) String date,      // 상세 페이지에서 전달받은 날짜
                              @RequestParam(required = false) String sessionId, // 상세 페이지에서 전달받은 회차
                              HttpServletRequest request,
                              Model model,
                              Principal principal) {

        org.springframework.security.web.csrf.CsrfToken csrfToken =
                (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());

        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }

        model.addAttribute("concertId", concertId);
        model.addAttribute("date", date);           // 모델에 추가하여 HTML에서 사용 가능하도록 함
        model.addAttribute("sessionId", sessionId); // 모델에 추가하여 HTML에서 사용 가능하도록 함

        Long userNo = null;
        if (principal != null) {
            userNo = userRepository.findByUserId(principal.getName())
                    .map(User::getUserNo)
                    .orElse(null);
        }
        model.addAttribute("userNo", userNo);

        return "concert/seatmap";
    }

    /**
     * 2. 공연 기본 정보 및 레이아웃 타입 조회 API
     */
    @ResponseBody
    @GetMapping("/api/concert/{concertId}")
    public ResponseEntity<?> getConcertInfo(@PathVariable String concertId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
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

            // 🌟 [추가] 로그인한 사용자가 이 공연에 대해 이미 보유 중인 티켓 매수를 함께 내려줘서
            // 프론트엔드(seatmap.js)가 "계정당 최대 4매" 제한을 정확하게 안내할 수 있도록 합니다.
            long myBookedCount = 0;
            if (userDetails != null) {
                Long userNo = userRepository.findByUserId(userDetails.getUsername())
                        .map(User::getUserNo)
                        .orElse(null);
                myBookedCount = seatService.getActiveTicketCount(userNo, concertId);
            }
            responseData.put("myBookedCount", myBookedCount);

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 3. 특정 공연의 회차별 좌석 배치 목록 조회 API (수정됨)
     */
    @ResponseBody
    @GetMapping({"/api/seats/{concertId}", "/api/{concertId}"})
    public ResponseEntity<List<Seat>> getSeatList(@PathVariable String concertId,
                                                  @RequestParam(required = false) String date,
                                                  @RequestParam(required = false) String sessionId) {
        System.out.println("====== 💺 [백엔드] 좌석 조회 요청: " + concertId + ", 날짜: " + date + ", 회차: " + sessionId);

        // 서비스에서 날짜와 회차 조건으로 좌석을 필터링하여 가져오도록 구현해야 합니다.
        List<Seat> seats = seatService.getSeatsBySchedule(concertId, date, sessionId);
        return ResponseEntity.ok(seats);
    }

    /**
     * 4. 공연별 좌석 배치 레이아웃 구조 코드 단독 조회
     */
    @ResponseBody
    @GetMapping("/layout/{concertId}")
    public ResponseEntity<String> getSeatLayoutType(@PathVariable String concertId) {
        return ResponseEntity.ok(seatService.getSeatLayoutType(concertId));
    }

    /**
     * 5. 실시간 웹소켓 기반 좌석 선택
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
     * 6. 실시간 웹소켓 기반 좌석 취소
     */
    @ResponseBody
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelSeat(@RequestBody Map<String, Object> data) {
        String seatId = data.get("seatId").toString();

        seatService.cancelSeat(seatId);
        return ResponseEntity.ok("좌석 취소 완료");
    }

    /**
     * 7. 좌석 상태값 직접 변경 제어
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
     */
    @ResponseBody
    @GetMapping("/price/{concertId}/{seatClass}")
    public ResponseEntity<Integer> getPrice(@PathVariable String concertId, @PathVariable String seatClass) {
        int price = seatService.calculatePrice(concertId, seatClass);
        return ResponseEntity.ok(price);
    }

    /**
     * 9. 프론트엔드 좌석 결제 준비 단계
     */
    @PostMapping("/api/booking/prepare")
    public ResponseEntity<?> prepareBooking(@RequestBody Map<String, Object> bookingData,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAIL");
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.findByUserId(userDetails.getUsername());
        Long userNo = user.getUserNo();

        try {
            Long realReservationKey = seatService.processBookingAndGetReservationKey(bookingData, userNo);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("bookingId", realReservationKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "FAIL");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/api/booking/cancel-ajax")
    @ResponseBody
    public ResponseEntity<Void> cancelBookingAjax(@RequestParam(value = "reservationKey", required = false) Long reservationKey) {
        if (reservationKey == null) return ResponseEntity.badRequest().build();

        try {
            Map<String, Object> cancelInfo = seatService.releaseTemporarySeatsWithInfo(reservationKey);
            if (cancelInfo != null && !cancelInfo.isEmpty()) {
                String concertId = (String) cancelInfo.get("concertId");
                String[] seatIds = (String[]) cancelInfo.get("seatIds");
                if (concertId != null && seatIds != null) {
                    for (String seatId : seatIds) {
                        Map<String, Object> cancelMessage = new HashMap<>();
                        cancelMessage.put("concertId", concertId);
                        cancelMessage.put("seatId", seatId.trim());
                        cancelMessage.put("type", "CANCELLED");
                        messagingTemplate.convertAndSend("/topic/seat/" + concertId, (Object) cancelMessage);
                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    /**
     * 추가: /seat-selection 경로를 위한 매핑
     */
    @GetMapping("/seat-selection")
    public String legacySeatSelectionPage(@RequestParam String concertId,
                                          @RequestParam String date,
                                          @RequestParam String sessionId,
                                          Model model) {
        // 기존 showSeatMap 로직을 여기에 동일하게 구현하거나
        // 혹은 아래와 같이 리다이렉트를 태울 수 있습니다.
        return "redirect:/seat/" + concertId + "?date=" + date + "&sessionId=" + sessionId;
    }
}