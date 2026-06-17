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
    //private final PaymentService paymentService;

    // 0. 결제 화면 (여기에 쿠폰, 예매 정보가 다 나옴)
    @GetMapping("/payment")
    public String showPaymentPage(Model model) {

        // 1. 현재 로그인한 사용자의 아이디를 가져옵니다.
        // (실제 프로젝트에서는 Spring Security 등을 통해 진짜 아이디를 가져와야 합니다.)
        // 지금은 연습용으로 "hong123"이라고 가정해 볼게요.
        String currentUserId = "hong123";

        // 2. Service에게 이 사람 아이디를 주면서 멤버십이 맞는지 물어봅니다.
        boolean isMember = bookingService.checkActiveMembership(currentUserId);

        // 3. 확인된 결과를 "isMember"라는 이름표를 붙여서 Model에 담습니다.
        // 이제 타임리프 화면에서 th:if="${isMember}" 를 쓸 수 있게 됩니다!
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
    @ResponseBody // 화면 이동 대신 URL 데이터만 던져줍니다!
    public String createBooking(@RequestBody BookingRequestDto requestDto) {

        // ⭐️ 내 눈으로 직접 백엔드가 받은 금액 확인해보기!
        System.out.println("=========================================");
        System.out.println("✅ 프론트에서 무사히 도착한 금액: " + requestDto.getPayAmount());
        System.out.println("=========================================");

        String lemonSqueezyUrl = bookingService.createTemporaryPayment(requestDto);

        return lemonSqueezyUrl;
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
                // 💡 discountRate -> couponDiscountRate 로 변경!
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