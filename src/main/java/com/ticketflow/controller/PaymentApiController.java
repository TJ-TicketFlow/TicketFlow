package com.ticketflow.controller;

import com.ticketflow.dto.RefundEligibilityDto;
import com.ticketflow.entity.User;
import com.ticketflow.service.MembershipService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentApiController {

    private final UserService userService;
    private final MembershipService membershipService;

    @PostMapping("/refund")
    public RefundEligibilityDto refund(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUserId(userDetails.getUsername());
        return membershipService.processRefund(user);
    }
}
