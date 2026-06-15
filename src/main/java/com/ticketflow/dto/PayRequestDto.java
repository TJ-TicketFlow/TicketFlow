package com.ticketflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PayRequestDto {
    private Long reservationKey;
    private String payName;
    private String buyerName;
    private String buyerEmail;
    private Long userCouponId;
}
