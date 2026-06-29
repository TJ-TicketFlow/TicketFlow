package com.ticketflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.Map;

@Getter @Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookRequestDto {
    private WebhookMeta meta;
    private WebhookData data;

    @Getter @Setter
    @ToString
    public static class WebhookMeta {
        private String event_name;
        private Map<String, Object> custom_data;
    }

    @Getter @Setter
    @ToString
    public static class WebhookData {
        private String id;
        private String type;
        private WebhookAttributes attributes;
        private Map<String, Object> metadata;
    }

    @Getter @Setter
    @ToString
    public static class WebhookAttributes {
        private Long customer_id;
        private Long variant_id;
        private Long order_id;
        private String status;
        private String renews_at;
        private String ends_at;
        private String created_at;
        private String user_email;
        private Integer order_number;
        private Integer total;
        private String card_brand;
        private String card_last_four;
    }
}