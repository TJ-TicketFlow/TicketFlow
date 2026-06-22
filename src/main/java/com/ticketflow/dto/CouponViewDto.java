package com.ticketflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CouponViewDto {
    private String type;
    private Integer discount;
    private LocalDate startDate;
    private LocalDate expireDate;
    private long daysLeft;
}