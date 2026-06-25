package com.ticketflow.controller;

import com.ticketflow.dto.CouponViewDto;
import com.ticketflow.dto.PaymentViewDto;
import com.ticketflow.dto.RefundEligibilityDto;
import com.ticketflow.dto.UserUpdateDto;
import com.ticketflow.entity.Membership;
import com.ticketflow.entity.MembershipPayments;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.MembershipPaymentRepository;
import com.ticketflow.repository.MembershipRepository;
import com.ticketflow.repository.UserCouponRepository;
import com.ticketflow.service.BookingService;
import com.ticketflow.service.LemonSqueezyRefundService;
import com.ticketflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import com.ticketflow.service.MembershipService;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final UserCouponRepository userCouponRepository;
    private final BookingService bookingService;
    private final MembershipService membershipService;
    private final LemonSqueezyRefundService lemonSqueezyRefundService;
    private final MembershipRepository membershipRepository;
    private final MembershipPaymentRepository paymentRepository;



    @GetMapping
    public String mypage() {
        return "redirect:/mypage/benefits";
    }

    @GetMapping("/benefits")
    public String mypageBenefits(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);

        List<CouponViewDto> coupons = getAvailableCoupons(user);
        model.addAttribute("coupons", coupons);
        model.addAttribute("availableCouponCount", coupons.stream().filter(c -> c.getStatus() == 0).count());


        LocalDate endDate = java.time.LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);

        org.springframework.data.domain.Page<java.util.Map<String, Object>> ticketPage =
                bookingService.getMyTicketHistory(userDetails.getUsername(), startDate, endDate, org.springframework.data.domain.PageRequest.of(0, 2));

        model.addAttribute("tickets", ticketPage.getContent());                // 최근 내역 2개
        model.addAttribute("totalTicketCount", ticketPage.getTotalElements());
        model.addAttribute("coupons", getAvailableCoupons(user));
        return "mypage/mypage_benefits";
    }

//  coupon
    @GetMapping("/coupons")
    public String mypageCoupons(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        addUserAndCouponsToModel(user, model);

        return "mypage/mypage_coupons";
    }
    private void addUserAndCouponsToModel(User user, Model model) {
        model.addAttribute("user", user);
        List<CouponViewDto> coupons = getAvailableCoupons(user);
        model.addAttribute("coupons", coupons);
        model.addAttribute("availableCouponCount", coupons.stream().filter(c -> c.getStatus() == 0).count());
    }
    private List<CouponViewDto> getAvailableCoupons(User user) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUser(user);

        return userCoupons.stream()
                .sorted(Comparator.comparing(UserCoupon::getUserCouponExpireAt))
                .map(uc -> new CouponViewDto(
                        uc.getCoupon().getCouponName(),
                        uc.getCoupon().getCouponDiscountRate(),
                        uc.getUserCouponIssuedAt().toLocalDate(),
                        uc.getUserCouponExpireAt().toLocalDate(),
                        Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), uc.getUserCouponExpireAt().toLocalDate())),
                        uc.getUserCouponStatus()
                ))
                .collect(Collectors.toList());
    }
//  membership
    @GetMapping("/membership/payments")
    public String mypageMembershipPayments(@AuthenticationPrincipal UserDetails userDetails,
                                           Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);

        Membership membership = membershipRepository.findByUser(user)
                .stream().findFirst().orElse(null);

        List<PaymentViewDto> payments = membership == null
                ? Collections.emptyList()
                : paymentRepository.findByMembershipOrderByMembershipHistoryDateDesc(membership)
                .stream()
                .map(this::toPaymentViewDto)
                .collect(Collectors.toList());

        model.addAttribute("payments", payments);
        model.addAttribute("refundEligibility", membershipService.checkRefundEligibility(user));

        if (membership != null && membership.getMembershipCustomerId() != null) {
            try {

                String portalUrl = lemonSqueezyRefundService.getCustomerPortalUrl(membership.getMembershipCustomerId());
                model.addAttribute("portalUrl", portalUrl);
            } catch (Exception e) {
                System.out.println("⚠️ Customer Portal URL 조회 실패: " + e.getMessage());
            }
        }

        return "mypage/mypage_membership_history";
    }

    @GetMapping("/membership")
    public String mypageMembership(@AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);

        Membership membership = membershipRepository.findByUser(user).stream()
                .max(java.util.Comparator.comparing(Membership::getMembershipId))
                .orElse(null);

        if (membership != null) {
            model.addAttribute("memInfo", membership);
        }

        return "mypage/mypage_membership";
    }

    @GetMapping("/membership/subscribe")
    public String mypageMembershipSubscribe(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestParam(required = false) String plan,
                                            Model model,
                                            RedirectAttributes rttr) {

        User user = userService.findByUserId(userDetails.getUsername());

        if ("premium".equals(user.getMembership())) {
            rttr.addFlashAttribute("errorMessage", "이미 프리미엄 멤버십을 이용 중입니다.");
            return "redirect:/mypage/membership";
        }

        model.addAttribute("user", user);
        model.addAttribute("plan", plan);

        return "mypage/mypage_membership_subscribe";
    }

    @PostMapping("/membership/cancel")
    public String mypageMembershipCancel(@AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes rttr) {
        User user = userService.findByUserId(userDetails.getUsername());
        RefundEligibilityDto result = membershipService.cancel(user);

        if (result.isEligible()) {
            rttr.addFlashAttribute("successMessage", result.getReason());
        } else {
            rttr.addFlashAttribute("errorMessage", result.getReason());
        }
        return "redirect:/mypage/benefits";
    }

    @GetMapping("/profile")
    public String mypageProfile(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/mypage_profile";
    }

    @PostMapping("/profile")
    public String mypageProfileSave(@AuthenticationPrincipal UserDetails userDetails,
                                    @ModelAttribute UserUpdateDto dto,
                                    RedirectAttributes rttr) {
        try {
            userService.updatePassword(userDetails.getUsername(), dto);
            rttr.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            rttr.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/mypage/profile";
    }

    @GetMapping("/tickets")
    public String mypageTickets(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                @PageableDefault(size = 5) Pageable pageable, // 💡 한 페이지에 n개씩!
                                Model model) {

        // 💡 1. 날짜 기본값 세팅 (1달 전 ~ 오늘)
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusMonths(1);

        String userId = userDetails.getUsername();
        model.addAttribute("user", userService.findByUserId(userId));

        // 💡 2. 진짜 페이징된 데이터 가져오기
        Page<Map<String, Object>> ticketPage = bookingService.getMyTicketHistory(userId, startDate, endDate, pageable);

        model.addAttribute("tickets", ticketPage.getContent()); // 실제 데이터 목록
        model.addAttribute("page", ticketPage);                 // 페이지네이션용 정보 (총 페이지 수 등)
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "mypage/mypage_tickets";
    }

    @GetMapping("/tickets/{bookingNo}")
    public String mypageTicketDetail(@PathVariable("bookingNo") String bookingNo,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        Long payNo = Long.valueOf(bookingNo);
        try {
            // 💡 서비스에 결제 번호와 함께 '내 고유 번호(user.getUserNo())'도 같이 보냅니다!
            Map<String, Object> ticketDetail = bookingService.getTicketDetail(payNo, user.getUserNo());
            model.addAttribute("ticket", ticketDetail);

            return "mypage/mypage_ticket_detail";

        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("🚨 잘못된 예매 상세 접근 차단: " + e.getMessage());

            // 남의 내역을 훔쳐보려고 주소창을 장난쳤다면, 마이페이지 리스트 페이지로 강제 추방합니다.
            return "redirect:/";
        }
    }

    private PaymentViewDto toPaymentViewDto(MembershipPayments p) {
        String statusLabel;
        String statusClass;
        switch (p.getPaymentStatus()) {
            case "PAID":
                statusLabel = "결제완료";
                statusClass = "status-booked";
                break;
            case "FAILED":
                statusLabel = "결제실패";
                statusClass = "status-pending";
                break;
            case "REFUNDED":
                statusLabel = "환불완료";
                statusClass = "status-cancel";
                break;
            default:
                statusLabel = p.getPaymentStatus();
                statusClass = "status-pending";
        }

        String cardInfo = (p.getCardBrand() != null && !"UNKNOWN".equals(p.getCardBrand()))
                ? p.getCardBrand().toUpperCase() + " ****" + p.getCardLastFour()
                : "-";

        return new PaymentViewDto(
                p.getMembershipHistoryDate(),
                p.getMembershipOrderId(),
                p.getMembershipPayAmount(),
                cardInfo,
                statusLabel,
                statusClass
        );
    }


    @GetMapping("/wishlist")
    public String mypageWishlist(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/mypage_wishlist";
    }

    @GetMapping("/withdraw")
    public String withdraw(@AuthenticationPrincipal UserDetails userDetails,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           RedirectAttributes rttr) {
        userService.withdraw(userDetails.getUsername());
        new SecurityContextLogoutHandler().logout(request, response, null);
        rttr.addFlashAttribute("successMessage", "탈퇴가 완료되었습니다.");
        return "redirect:/login";
    }
}