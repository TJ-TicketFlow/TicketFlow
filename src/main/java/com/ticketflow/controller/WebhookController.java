package com.ticketflow.controller;

import com.ticketflow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
//예매 결제
public class WebhookController {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper; // JSON 데이터를 쉽게 읽게 해주는 스프링의 기본 도구입니다.

    // 1단계에서 설정한 비밀번호를 가져옵니다.
    @Value("${lemonsqueezy.webhook-secret2}")
    private String webhookSecret;

    // 예매 결제 웹훅
    @PostMapping("/booking/webhooks")
    public ResponseEntity<String> handleBookingWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature) {

        try {
            // 1. 보안 검사: 진짜 레몬스퀴즈가 보낸 게 맞는지 확인
            if (!isValidSignature(payload, signature)) {
                System.out.println("❌ 웹훅 서명 검증 실패! 해커의 공격일 수 있습니다.");
                return ResponseEntity.status(403).body("잘못된 접근입니다.");
            }

            // 2. JSON 포장지 뜯기
            JsonNode rootNode = objectMapper.readTree(payload);

            // 어떤 이벤트인지 확인 (예: order_created 가 오면 결제 성공이라는 뜻)
            String eventName = rootNode.path("meta").path("event_name").asText();

            if ("order_created".equals(eventName)) {

                // 1. 커스텀 데이터와 레몬스퀴지 기본 주문번호 꺼내기
                String merchantUid = rootNode.path("meta").path("custom_data").path("merchant_uid").asText();
                String lsOrderId = rootNode.path("data").path("id").asText();

                // ⭐️ 2. 여기서부터 추가! JSON의 'attributes' (상세 정보) 칸을 엽니다.
                JsonNode attributes = rootNode.path("data").path("attributes");

                String currency = attributes.path("currency").asText(); // "KRW"
                String lsCustomerId = attributes.path("customer_id").asText(); // 고객 ID
                String receiptUrl = attributes.path("urls").path("receipt").asText(); // 영수증 URL

                // 이벤트 ID는 meta 안에 들어있습니다.
                String lsWebhookEventId = rootNode.path("meta").path("webhook_id").asText();

                // first_order_item 안에 들어있는 순수 price를 꺼냅니다! (3200000)
                long purePriceCent = attributes.path("first_order_item").path("price").asLong();
                // 우리가 보낼 때 100을 곱했으니, 비교할 때는 100으로 다시 나눠서 원상복구(32000) 시킵니다!
                long webhookAmount = purePriceCent / 100;

                // 3. 서비스에게 "이것들도 다 같이 장부에 적어줘!" 라고 넘깁니다.
                bookingService.completePayment(merchantUid, lsOrderId, currency, lsCustomerId, receiptUrl, lsWebhookEventId, webhookAmount);
            }

            // 5. 레몬스퀴즈에게 "알림 잘 받았어! 고마워!" 라고 200 OK 보내기
            return ResponseEntity.ok("Success");

        } catch (Exception e) {
            e.printStackTrace();
            // 에러가 나면 레몬스퀴즈에게 500 에러를 보내서 "나중에 다시 알림 보내줘" 라고 합니다.
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }
    // ==========================================
    // [해독기] 암호(Signature)가 맞는지 확인하는 마법의 메서드
    // ==========================================
    private boolean isValidSignature(String payload, String signature) {
        try {
            // HmacSHA256 이라는 암호화 방식을 준비합니다.
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            // 알림 내용(payload)과 비밀번호를 섞어서 우리만의 암호를 만듭니다.
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // 만들어진 암호를 예쁜 문자열(Hex)로 바꿉니다.
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // 레몬스퀴지가 보낸 암호(signature)와 우리가 만든 암호가 똑같은지 비교합니다!
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }


    // 멤버십 결제
    @PostMapping("/payment/webhook")
    public ResponseEntity<?> handlePaymentWebhook(){
        return ResponseEntity.ok(Map.of("message", "webhook"));
    }
}

