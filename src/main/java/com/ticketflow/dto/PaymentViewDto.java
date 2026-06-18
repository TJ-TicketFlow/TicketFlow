package com.ticketflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentViewDto {
    private LocalDateTime paidAt;
    private String orderId;
    private Integer amount;
    private String cardInfo;
    private String statusLabel;
    private String statusClass;
}