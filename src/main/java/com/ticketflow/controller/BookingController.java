package com.ticketflow.controller;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.service.BookingService;
import lombok.RequiredArgsConstructor;
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
            // 💡 수정된 부분: 열쇠가 안 들어오면 에러를 내지 말고, 일단 1번으로 쳐줄게! (테스트용)
            @RequestParam(value = "reservationKey", required = false, defaultValue = "1") Long reservationKey,
            Model model) {

        // 1. 현재 로그인한 사용자의 고유 번호 (임시 1번)
        Long currentUserNo = 1L;

        // 💡 2. 서비스에게 회원 정보 포장 상자를 가져오라고 시킵니다.
        Map<String, Object> buyer = bookingService.getUserInfoMap(currentUserNo);
        model.addAttribute("user", buyer);

        // 💡 3. 서비스에게 티켓 정보 포장 상자를 가져오라고 시킵니다.
        Map<String, Object> ticket = bookingService.getTicketInfoMap(reservationKey);
        model.addAttribute("ticket", ticket);

        // 💡 4. 자바스크립트가 fetch 통신할 때 쓸 수 있게 예약 번호도 몰래 넘겨줍니다.
        model.addAttribute("reservationKey", reservationKey);

        // 5. 멤버십 확인 (추후 currentUserNo와 맞춰서 userId 조회가 필요할 수 있습니다)
        boolean isMember = bookingService.checkActiveMembership("hong123");
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
    public String createBooking(@RequestBody BookingRequestDto requestDto) {
        System.out.println("=========================================");
        System.out.println("✅ 프론트에서 무사히 도착한 금액: " + requestDto.getPayAmount());
        System.out.println("=========================================");

        return bookingService.createTemporaryPayment(requestDto);
    }

    // 2. 멤버십 확인
    @GetMapping("/checkmembership")
    @ResponseBody
    public String checkMembership() {
        return "VIP";
    }

    // 3. 쿠폰 목록 가져오기
    @GetMapping("/checkcoupons")
    @ResponseBody
    public List<Map<String, Object>> getCoupons() {
        return List.of(
                Map.of("userCouponId", 101, "name", "신규 가입 5% 할인", "couponDiscountRate", 5),
                Map.of("userCouponId", 102, "name", "멤버십 10% 할인", "couponDiscountRate", 10)
        );
    }

    // 5. 결제 확인
    @GetMapping("/paycheck")
    @ResponseBody
    public Boolean checkPay() {
        return true;
    }

    // 9. 예매 취소 가능 여부 확인
    @GetMapping("/{id}/cancelable")
    @ResponseBody
    public Boolean checkCancelable(@PathVariable("id") Long id) {
        return true;
    }

    // 10. 예매 취소
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public String cancelBooking(@PathVariable("id") Long id) {
        return "예매가 성공적으로 취소 되었습니다";
    }

    // 11. 예매 완료 이메일/알림
    @PostMapping("/notification")
    @ResponseBody
    public String sendNotification() {
        return "예매 완료 이메일 및 알림을 전송합니다.";
    }
}