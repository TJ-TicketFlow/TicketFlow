package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;

import com.ticketflow.entity.SelectedSeat;
import com.ticketflow.entity.User;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
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

    private final SeatRepository seatRepository;

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
    @Transactional // 💡 데이터 정합성을 위해 트랜잭션 어노테이션을 붙여줍니다.
    // 1. 매개변수 맨 뒤에 'Principal principal'을 추가해 로그인 정보를 확보합니다.
    public ResponseEntity<?> prepareBooking(@RequestBody com.ticketflow.dto.selectedSeat request, Principal principal) {
        System.out.println("====== ✈️ [백엔드] 예매 데이터 수신 및 DB 처리 시작 ======");

        // 2. [인증 검증] 로그인 상태가 아니라면 즉시 차단합니다.
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인 세션이 만료되었습니다. 다시 로그인해주세요."));
        }

        // 3. 로그인한 유저의 ID(아이디 또는 이메일)로 실제 User 엔티티 객체를 조회합니다.
        String username = principal.getName();
        User currentUser = userRepository.findByUserId(username) // 💡 테이블 설계(findByUserEmail 등)에 맞게 커스텀 필요
                .orElseThrow(() -> new RuntimeException("현재 로그인된 회원 정보를 찾을 수 없습니다."));

        // ----------------------------------------------------
        // 🎫 [케이스 1] 지정석(SEAT) 예매 처리
        // ----------------------------------------------------
        if ("SEAT".equalsIgnoreCase(request.getTicketType()) && request.getSelectedSeats() != null) {
            for (String seatId : request.getSelectedSeats()) {
                String dbFormatId = seatId.replace("SEAT_", "").replace("R", "").replace("C", "");
                String realDbSeatId = request.getConcertId() + "_" + dbFormatId;

                Seat seat;
                String detectedClass = "STANDARD";

                String rowVal = "1";
                String colVal = "1";
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("R([A-Za-z0-9]+)_C(\\d+)").matcher(seatId);
                if (matcher.find()) {
                    rowVal = matcher.group(1);
                    colVal = matcher.group(2);
                }

                try {
                    seat = seatService.getSeatById(realDbSeatId);
                    if (seat.getSeatClass() != null) {
                        detectedClass = seat.getSeatClass();
                    }
                } catch (RuntimeException e) {
                    seat = new Seat();
                    seat.setSeatId(realDbSeatId);
                    Concert dummyConcert = new Concert();
                    dummyConcert.setConcertId(request.getConcertId());
                    seat.setConcert(dummyConcert);
                    seat.setSeatStatus((short) 1);
                    detectedClass = rowVal;
                    seat.setSeatClass(detectedClass);
                    seat.setSeatRow(rowVal);
                    seat.setSeatCol(colVal);
                    seat = seatRepository.save(seat);
                }

                if (seat.getSeatStatus() == 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "이미 선택된 좌석입니다."));
                }

                seat.setSeatStatus((short) 0);
                seatRepository.save(seat);

                SelectedSeat selectedSeat = new SelectedSeat();
                selectedSeat.setSeat(seat);

                Concert currentConcert = new Concert();
                currentConcert.setConcertId(request.getConcertId());
                selectedSeat.setConcert(currentConcert);

                // 🎯 [해결 1] 'user_no'가 null이 되지 않도록 조회한 유저 객체를 주입합니다.
                selectedSeat.setUser(currentUser);

                int finalPrice = 0;
                try {
                    finalPrice = seatService.calculatePrice(request.getConcertId(), detectedClass);
                } catch (Exception ex) {
                    try {
                        Concert c = concertService.findById(request.getConcertId());
                        if (c.getConcertPriceInfo() != null) {
                            String[] pTokens = c.getConcertPriceInfo().split(",");
                            finalPrice = Integer.parseInt(pTokens[0].split(":")[1].trim());
                        }
                    } catch (Exception ex2) {
                        finalPrice = 0;
                    }
                }

                // 🎯 [해결 2] int형 변수를 Long 객체 형태로 안전하게 감싸 매핑 에러를 방지합니다.
                selectedSeat.setPrice(Long.valueOf(finalPrice));

                selectedSeatRepository.save(selectedSeat);
            }
        }

        // ----------------------------------------------------
        // 🏃 [케이스 2] 스탠딩(STANDING) 예매 처리
        // ----------------------------------------------------
        else if ("STANDING".equalsIgnoreCase(request.getTicketType()) && request.getQuantities() != null) {
            for (Map.Entry<String, Integer> entry : request.getQuantities().entrySet()) {
                String grade = entry.getKey();
                int quantity = entry.getValue();

                for (int i = 1; i <= quantity; i++) {
                    String realDbSeatId = request.getConcertId() + "_" + grade + "_STAND_" + i;

                    Seat seat;
                    try {
                        seat = seatService.getSeatById(realDbSeatId);
                    } catch (RuntimeException e) {
                        seat = new Seat();
                        seat.setSeatId(realDbSeatId);
                        Concert dummyConcert = new Concert();
                        dummyConcert.setConcertId(request.getConcertId());
                        seat.setConcert(dummyConcert);
                        seat.setSeatStatus((short) 1);
                        seat.setSeatClass(grade);
                        seat.setSeatRow("STAND");
                        seat.setSeatCol(String.valueOf(i));
                        seat = seatRepository.save(seat);
                    }

                    if (seat.getSeatStatus() == 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "해당 구역의 티켓이 매진되었습니다."));
                    }

                    seat.setSeatStatus((short) 0);
                    seatRepository.save(seat);

                    SelectedSeat selectedSeat = new SelectedSeat();
                    selectedSeat.setSeat(seat);

                    Concert currentConcert = new Concert();
                    currentConcert.setConcertId(request.getConcertId());
                    selectedSeat.setConcert(currentConcert);

                    // 🎯 [해결 1] 스탠딩 분기도 똑같이 유저 객체를 주입합니다.
                    selectedSeat.setUser(currentUser);

                    int standingPrice = 0;
                    try {
                        standingPrice = seatService.calculatePrice(request.getConcertId(), grade);
                    } catch (Exception ex) {
                        try {
                            Concert c = concertService.findById(request.getConcertId());
                            if (c.getConcertPriceInfo() != null) {
                                String[] pTokens = c.getConcertPriceInfo().split(",");
                                standingPrice = Integer.parseInt(pTokens[0].split(":")[1].trim());
                            }
                        } catch (Exception ex2) {
                            standingPrice = 0;
                        }
                    }

                    // 🎯 [해결 2] 스탠딩 가격 또한 Long 객체 형태로 감싸서 주입합니다.
                    selectedSeat.setPrice(Long.valueOf(standingPrice));

                    selectedSeatRepository.save(selectedSeat);
                }
            }
        }

        System.out.println("====== 🎉 [백엔드] 지정석/스탠딩 예매 데이터 정상 처리 완료 ======");

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