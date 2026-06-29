package com.ticketflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequestDto {
    private long reservationKey;
    private String merchantUid;
    private String payName;
    @JsonProperty("payAmount")
    private Long payAmount;
    private Long userCouponId;
    private String buyerName;
    private String buyerEmail;
    private String payDelName;
    private String payDelCall;
    private String payDelPostcode;
    private String payDelAddr;
    private String payStatus;
    private String captchaKey;
    private String captchaValue;
}