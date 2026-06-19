package com.ticketflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LemonSqueezy 환불(Refund) API 호출 전용 서비스.
 * - 일회성 주문(orders) / 구독 인보이스(subscription-invoices) 환불을 모두 처리.
 * - 이 클래스는 "LemonSqueezy와의 통신"만 책임진다.
 *   소유권 검증, DB 상태 갱신은 PayService / MembershipService 쪽에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    @Value("${lemon-squeezy.api-key}")
    private String LS_API_KEY;

    public enum RefundTarget {
        ORDER("orders", "https://api.lemonsqueezy.com/v1/orders/%s/refund"),
        SUBSCRIPTION_INVOICE("subscription-invoices", "https://api.lemonsqueezy.com/v1/subscription-invoices/%s/refund");

        final String type;
        final String urlTemplate;

        RefundTarget(String type, String urlTemplate) {
            this.type = type;
            this.urlTemplate = urlTemplate;
        }
    }

    /**
     * @param target        ORDER 또는 SUBSCRIPTION_INVOICE
     * @param lsId          LemonSqueezy 측 order id 또는 subscription-invoice id (우리 DB의 PK가 아님)
     * @param amountInCents null이면 전액 환불, 값이 있으면 해당 금액만 부분 환불
     * @return LemonSqueezy 응답 바디 (data.attributes.refunded, refunded_amount, refunded_at 포함)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> refund(RefundTarget target, String lsId, Integer amountInCents) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.set("Accept", "application/vnd.api+json");
        headers.setBearerAuth(LS_API_KEY);

        Map<String, Object> attributes = new HashMap<>();
        if (amountInCents != null) {
            attributes.put("amount", amountInCents);
        }

        Map<String, Object> dataNode = new HashMap<>();
        dataNode.put("type", target.type);
        dataNode.put("id", lsId);
        dataNode.put("attributes", attributes);

        Map<String, Object> body = new HashMap<>();
        body.put("data", dataNode);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = String.format(target.urlTemplate, lsId);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("LemonSqueezy 환불 API 오류: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public Map<String, Object> refundOrder(String lsOrderId, Integer amountInCents) {
        return refund(RefundTarget.ORDER, lsOrderId, amountInCents);
    }

    public Map<String, Object> refundSubscriptionInvoice(String lsInvoiceId, Integer amountInCents) {
        return refund(RefundTarget.SUBSCRIPTION_INVOICE, lsInvoiceId, amountInCents);
    }
}
