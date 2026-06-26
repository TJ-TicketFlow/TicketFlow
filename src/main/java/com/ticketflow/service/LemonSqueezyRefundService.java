package com.ticketflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class LemonSqueezyRefundService {

    @Value("${lemonsqueezy.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

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

    public String refund(RefundTarget target, String id, Integer amountInCents) {
        Map<String, Object> attributes = new HashMap<>();
        if (amountInCents != null) {
            attributes.put("amount", amountInCents);
        }

        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "type", target.type,
                        "id", id,
                        "attributes", attributes
                )
        );

        return restClient.post()
                .uri(String.format(target.urlTemplate, id))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/vnd.api+json")
                .header("Content-Type", "application/vnd.api+json")
                .body(body)
                .retrieve()
                .body(String.class);
    }

    public String refundSubscriptionInvoice(String invoiceId, Integer amountInCents) {
        return refund(RefundTarget.SUBSCRIPTION_INVOICE, invoiceId, amountInCents);
    }

    public void cancelSubscription(String subscriptionId) {
        restClient.delete()
                .uri("https://api.lemonsqueezy.com/v1/subscriptions/{id}", subscriptionId)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/vnd.api+json")
                .retrieve()
                .toBodilessEntity();
    }

    public String getCustomerPortalUrl(String customerId) {
        Map<String, Object> response = restClient.get()
                .uri("https://api.lemonsqueezy.com/v1/customers/{id}", customerId)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/vnd.api+json")
                .retrieve()
                .body(Map.class);

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> attrs = (Map<String, Object>) data.get("attributes");
        Map<String, Object> urls = (Map<String, Object>) attrs.get("urls");
        return (String) urls.get("customer_portal");
    }

    public void resumeSubscription(String subId) throws Exception {
        String url = "https://api.lemonsqueezy.com/v1/subscriptions/" + subId;

        String requestBody = """
                {
                  "data": {
                    "type": "subscriptions",
                    "id": "%s",
                    "attributes": {
                      "cancelled": false
                    }
                  }
                }
                """.formatted(subId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.api+json")
                .header("Content-Type", "application/vnd.api+json")
                .header("Authorization", "Bearer " + apiKey)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("✅ 레몬스퀴즈 구독 재개 성공: " + subId);
        } else {
            System.err.println("❌ 레몬스퀴즈 구독 재개 실패: " + response.body());
            throw new RuntimeException("구독 재개 API 호출 실패");
        }
    }
}