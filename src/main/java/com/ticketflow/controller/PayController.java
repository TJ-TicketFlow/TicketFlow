package com.ticketflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PayController {

    @Value("${lemonsqueezy.api-key}")
    private String LS_API_KEY ;
    @Value("${lemonsqueezy.store-id}")
    private String LS_STORE_ID;

    // ==========================================================
    // 1. PAYMENT (결제 / 레몬스퀴지 연동)
    // ==========================================================
    @PostMapping("/payment/create-checkout")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, String> data,
                                            HttpSession session, HttpServletRequest request) {

        if (!Boolean.TRUE.equals(session.getAttribute("logged_in"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String email = data.get("email");
        String variantId = data.get("variantId");

        if (email == null || email.trim().isEmpty() || variantId == null || variantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일 또는 플랜 정보가 없습니다."));
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
            headers.set("Accept", "application/vnd.api+json");
            headers.setBearerAuth(LS_API_KEY);

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> dataNode = new HashMap<>();
            dataNode.put("type", "checkouts");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("checkout_data", Map.of("email", email));
            attributes.put("test_mode", true);

            // 콜백/리다이렉트 URL 동적 생성
            String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
            attributes.put("product_options", Map.of("redirect_url", baseUrl + "/mypage/benefits"));
            dataNode.put("attributes", attributes);

            Map<String, Object> relationships = new HashMap<>();
            relationships.put("store", Map.of("data", Map.of("type", "stores", "id", LS_STORE_ID)));
            relationships.put("variant", Map.of("data", Map.of("type", "variants", "id", variantId)));
            dataNode.put("relationships", relationships);

            body.put("data", dataNode);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.lemonsqueezy.com/v1/checkouts",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
                Map<String, Object> responseAttributes = (Map<String, Object>) responseData.get("attributes");
                String checkoutUrl = (String) responseAttributes.get("url");
                return ResponseEntity.ok(Map.of("url", checkoutUrl));
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body(Map.of("error", "LemonSqueezy API 오류"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "네트워크 오류: " + e.getMessage()));
        }
    }




    @GetMapping("/payment/history")
    public ResponseEntity<?> getPaymentHistory() {
        return ResponseEntity.ok(Map.of("message", "결제 내역 조회"));
    }

    @PostMapping("/payment/refund")
    public ResponseEntity<?> refundPayment() {
        return ResponseEntity.ok(Map.of("message", "환불 처리"));
    }

    // ==========================================================
    // 2. MEMBERSHIP (멤버십 상태 관리)
    // ==========================================================
    @GetMapping("/membership/status")
    public ResponseEntity<?> getMembershipStatus(HttpSession session) {
        return ResponseEntity.ok(Map.of("message", "내 멤버십 상태"));
    }

    @GetMapping("/membership/detail")
    public ResponseEntity<?> getMembershipDetail() {
        return ResponseEntity.ok(Map.of("message", "멤버십 상세 조회"));
    }

    @PostMapping("/membership/check")
    public ResponseEntity<?> checkMembership() {
        return ResponseEntity.ok(Map.of("message", "멤버십 유효성 체크"));
    }

    @PostMapping("/membership/cancel")
    public ResponseEntity<?> cancelMembership() {
        return ResponseEntity.ok(Map.of("message", "멤버십 해지"));
    }

    @PostMapping("/membership/reactivate")
    public ResponseEntity<?> reactivateMembership() {
        return ResponseEntity.ok(Map.of("message", "멤버십 재활성화"));
    }

    // ==========================================================
    // 3. COUPON (쿠폰 기능)
    // ==========================================================
    @PostMapping("/coupon/issue")
    public ResponseEntity<?> issueCoupon() {
        return ResponseEntity.ok(Map.of("message", "쿠폰 발급"));
    }

    @GetMapping("/coupon/list")
    public ResponseEntity<?> getCouponList() {
        return ResponseEntity.ok(Map.of("message", "쿠폰 목록 조회"));
    }

    @PostMapping("/coupon/validate")
    public ResponseEntity<?> validateCoupon() {
        return ResponseEntity.ok(Map.of("message", "쿠폰 유효성 검증"));
    }
}

