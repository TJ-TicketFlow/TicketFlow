package com.ticketflow.controller;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
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
    public String showPaymentPage() {

        // 1. Service에게 영수증 번호를 주면서 데이터 조립을 시킵니다.
        //PaymentPageResponseDto realPageData = payService.getPaymentInfo(reservationKey);

        // 2. Model에 진짜 데이터가 담긴 바구니를 올려서 화면으로 보냅니다.
        //model.addAttribute("paymentInfo", realPageData);

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
        // TODO: DB에서 유저의 쿠폰 목록과 할인율을 가져와서 던져줍니다.
        // 프론트엔드에서 이 데이터를 받아 목록을 그리고, 직접 계산합니다.

        // (임시 데이터 예시 - 나중에는 DTO와 Service를 사용하게 됩니다)
        return List.of(
                Map.of("couponId", 1, "name", "신규 가입 5% 할인", "discountRate", 5),
                Map.of("couponId", 2, "name", "멤버십 10% 할인", "discountRate", 10)
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