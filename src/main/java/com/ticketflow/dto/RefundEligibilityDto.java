package com.ticketflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefundEligibilityDto {
    private boolean eligible;
    private String reason;
}