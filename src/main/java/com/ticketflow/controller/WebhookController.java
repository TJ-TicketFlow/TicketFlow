package com.ticketflow.controller;

import com.ticketflow.dto.WebhookRequestDto;
import com.ticketflow.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@ToString
@RestController
@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
//예매 결제
public class WebhookController {

    @PostMapping("/booking/webhooks")
    public ResponseEntity<String> handlebookingWebhook(@RequestBody String payload,
                                                @RequestHeader("X-Signature") String signature) {

//        // 1. 보안 검사: 진짜 레몬스퀴즈가 보낸 게 맞는지 암호(Signature)를 확인합니다. 해커가 보낸 걸 수도 있으니까요!
//        if (!isValidSignature(payload, signature)) {
//            return ResponseEntity.status(403).body("잘못된 접근입니다.");
//        }
//
//        // 2. custom_data 확인: 아까 1단계에서 몰래 숨겨서 보냈던 우리 DB의 '주문 번호(myOrderId)'를 꺼냅니다.
//        Long myOrderId = extractOrderIdFromPayload(payload);
//
//        // 3. DB 업데이트: 드디어 해당 주문을 찾아 "결제 대기" -> "결제 완료" 로 상태를 바꿉니다!
//        bookingService.completePayment(myOrderId);
//
//        // 4. 레몬스퀴즈에게 "연락 잘 받았어!" 라고 200 OK 상태를 보냅니다.
        return ResponseEntity.ok("Success");
    }


    private final MembershipService membershipService;

    @PostMapping("/payment/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookRequestDto dto) {
        try {
            System.out.println("🔥 웹훅 수신됨, DTO 내용: " + dto);

            membershipService.processPaymentWebhook(dto);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            System.err.println("❌ 웹훅 처리 중 치명적 에러 발생!");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

