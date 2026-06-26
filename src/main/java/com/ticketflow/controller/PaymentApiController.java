package com.ticketflow.controller;

import com.ticketflow.dto.RefundEligibilityDto;
import com.ticketflow.entity.User;
import com.ticketflow.service.MembershipService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/resume")
    public ResponseEntity<String> resumeMembership(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByUserId(userDetails.getUsername());
            membershipService.resume(user); // 서비스에 상태 되돌리기 지시

            return ResponseEntity.ok("멤버십이 성공적으로 다시 활성화되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("오류 발생: " + e.getMessage());
        }
    }
}
