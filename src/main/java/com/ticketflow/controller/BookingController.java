package com.ticketflow.controller;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    // ==========================================
    // [화면 띄우기 구역]
    // ==========================================

    // 0. 결제 화면 (여기에 쿠폰, 예매 정보가 다 나옴)
    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam(value = "reservationKey", required = false, defaultValue = "1") Long reservationKey,
            //(실제로 테스트할때)@RequestParam(value = "reservationKey", required = true) Long reservationKey,
            java.security.Principal principal, // 💡 [핵심] 스프링이 로그인한 사람 정보를 여기로 넣어줍니다!
            Model model) {

        // 🚨 1. 로그인 상태 확인 (방어 로직)
        if (principal == null) {
            // 로그인을 안 하고 결제창 주소를 직접 치고 들어왔다면? 로그인 페이지로 쫓아냅니다!
            return "redirect:/login";
        }

        // 💡 2. 진짜 로그인한 사람의 아이디 가져오기 (예: "hong123"이 진짜로 들어옴)
        String currentUserId = principal.getName();

        // 💡 3. 문자 아이디를 서비스에 주고, 고유 번호(user_no)를 받아옵니다.
        Long currentUserNo = bookingService.getUserNoById(currentUserId);

        try {
            // 티켓 정보를 가져오면서, 남의 것이거나 이미 결제된 것이면 에러를 던집니다.
            Map<String, Object> ticket = bookingService.getTicketInfoMap(reservationKey, currentUserNo);
            model.addAttribute("ticket", ticket);

            // 유저 정보 가져오기
            Map<String, Object> buyer = bookingService.getUserInfoMap(currentUserNo);
            model.addAttribute("user", buyer);

        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("🚨 잘못된 결제창 접근 차단 완료: " + e.getMessage());
            return "redirect:/"; // 문제 있으면 즉시 메인으로 쫓아냄
        }

        // 💡 4. 자바스크립트가 fetch 통신할 때 쓸 수 있게 예약 번호도 몰래 넘겨줍니다.
        model.addAttribute("reservationKey", reservationKey);

        // 5. 멤버십 확인 (추후 currentUserNo와 맞춰서 userId 조회가 필요할 수 있습니다)
        boolean isMember = bookingService.checkActiveMembership(currentUserId);
        model.addAttribute("isMember", isMember);

        return "booking/payment";
    }

    // 6. 결제 완료 페이지
    @GetMapping("/payresult")
    public String payResultPage() {
        return "booking/payresult";
    }

    // ==========================================
    // [데이터만 던져주는 구역 - @ResponseBody 필수!]
    // ==========================================

    // 1. 임시 예매 생성 및 레몬스퀴즈 창 띄우기
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<String> createBooking(@RequestBody BookingRequestDto requestDto, java.security.Principal principal) {

        System.out.println("=========================================");
        System.out.println("✅ 프론트에서 무사히 도착한 금액: " + requestDto.getPayAmount());
        System.out.println("=========================================");

        // 🚨 1. 로그인 안 한 사람 차단
        if (principal == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // 🚨 2. [핵심 방어막] 이미 결제된 티켓을 자바스크립트로 강제로 결제하려는 해커 차단!
        if (bookingService.isAlreadyPaid(requestDto.getReservationKey())) {
            System.out.println("🚨 API 통신을 통한 이중 결제 시도 차단됨!");
            return ResponseEntity.status(400).body("이미 결제가 완료된 예매건입니다.");
        }

        // 3. 정상적이면 레몬스퀴지 주소 생성해서 리턴!
        String checkoutUrl = bookingService.createTemporaryPayment(requestDto);
        return ResponseEntity.ok(checkoutUrl);
    }

    // 3. 쿠폰 목록 가져오기
    @GetMapping("/checkcoupons")
    @ResponseBody
    public List<Map<String, Object>> getCoupons(java.security.Principal principal) { // 💡 여기도 Principal 추가!

        // 🚨 로그인을 안 했으면 빈 상자(쿠폰 없음)를 던져줍니다.
        if (principal == null) {
            return List.of();
        }

        // 💡 진짜 로그인한 아이디 추출
        String currentUserId = principal.getName();

        // 진짜 아이디로 쿠폰 검색 후 반환
        return bookingService.getMyAvailableCoupons(currentUserId);
    }

    // 9. 예매 취소 가능 여부 (수수료 및 환불금액 미리보기)
    @GetMapping("/{id}/cancelable")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkCancelable(@PathVariable("id") Long id) {
        try {
            // 서비스에서 수수료 정보를 받아옵니다.
            Map<String, Object> feeInfo = bookingService.getCancelFeeInfo(id);
            return ResponseEntity.ok(feeInfo);
        } catch (Exception e) {
            // 에러가 나면 프론트엔드에 에러 메시지를 보냅니다.
            return ResponseEntity.badRequest().body(Map.of("cancelable", false, "message", e.getMessage()));
        }
    }

    // 10. 예매 취소 (진짜 취소 실행!)
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<String> cancelBooking(@PathVariable("id") Long id) {
        try {
            // 이전에 만들었던 취소(상태변경+좌석복구) 로직 실행
            bookingService.cancelTicket(id);
            return ResponseEntity.ok("성공적으로 취소되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // 11. 예매 완료 이메일/알림
    @PostMapping("/notification")
    @ResponseBody
    public String sendNotification() {
        return "예매 완료 이메일 및 알림을 전송합니다.";
    }

    // ==========================================
    // 💡 [네이버 캡차 2] 프론트엔드에 열쇠 던져주기
    // ==========================================
    @GetMapping("/captcha-key")
    @ResponseBody
    public Map<String, String> getCaptchaKey() {
        // 서비스에서 열쇠를 받아옵니다.
        String key = bookingService.getNaverCaptchaKey();

        // 프론트엔드가 이미지 주소를 바로 조립할 수 있도록 함께 넘겨줍니다.
        return Map.of(
                "key", key,
                "imageUrl", "https://openapi.naver.com/v1/captcha/ncaptcha.bin?key=" + key
        );
    }

    // ==========================================
    // 💡 12. 결제창 이탈 시 좌석 해제 API
    // ==========================================
    @PostMapping("/release-seat")
    @ResponseBody
    public ResponseEntity<String> releaseSeat(@RequestBody Map<String, Long> payload, java.security.Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().build();

        Long reservationKey = payload.get("reservationKey");
        if (reservationKey != null) {
            // 서비스 로직 실행
            bookingService.releaseUnpaidSeat(reservationKey);
        }
        return ResponseEntity.ok("Seat released");
    }
}