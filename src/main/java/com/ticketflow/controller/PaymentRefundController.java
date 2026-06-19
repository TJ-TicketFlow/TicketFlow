package com.ticketflow.controller;

import com.ticketflow.entity.User;
import com.ticketflow.service.MembershipPaymentRefundService;
import com.ticketflow.service.PayService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 마이페이지 - 본인 결제 환불.
 * /mypage/** 는 SecurityConfig 에서 이미 authenticated() 로 보호되므로
 * 별도 permitAll/권한 설정이 필요 없다.
 * 단, "인증된 사용자라면 누구나" 호출 가능하므로 서비스 계층에서 반드시
 * 결제 소유자 == 로그인 사용자 검증을 한다 (PayService / MembershipPaymentRefundService 참고).
 */
@org.springframework.stereotype.Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class PaymentRefundController {

    private final UserService userService;
    private final PayService payService;
    private final MembershipPaymentRefundService membershipPaymentRefundService;

    // 일회성 결제(좌석/티켓 등) 환불
    @PostMapping("/payments/{payNo}/refund")
    public String refundPayment(@PathVariable Long payNo,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes rttr) {

        User user = userService.findByUserId(userDetails.getUsername());

        try {
            payService.refundMyPayment(payNo, user);
            rttr.addFlashAttribute("successMessage", "환불이 완료되었습니다.");
        } catch (SecurityException e) {
            rttr.addFlashAttribute("errorMessage", "본인 결제 내역만 환불할 수 있습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            rttr.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMessage", "환불 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/tickets";
    }

    // 멤버십 정기결제(구독 인보이스) 환불
    @PostMapping("/membership/payments/{paymentId}/refund")
    public String refundMembershipPayment(@PathVariable Long paymentId,
                                           @AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes rttr) {

        User user = userService.findByUserId(userDetails.getUsername());

        try {
            membershipPaymentRefundService.refundMyMembershipPayment(paymentId, user);
            rttr.addFlashAttribute("successMessage", "환불이 완료되었습니다.");
        } catch (SecurityException e) {
            rttr.addFlashAttribute("errorMessage", "본인 결제 내역만 환불할 수 있습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            rttr.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMessage", "환불 처리 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/membership/history";
    }
}
