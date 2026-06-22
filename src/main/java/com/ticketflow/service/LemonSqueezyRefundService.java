package com.ticketflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
}