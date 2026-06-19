package com.ticketflow.controller;

import com.ticketflow.dto.UserUpdateDto;
import com.ticketflow.entity.User;
import com.ticketflow.service.BookingService;
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
import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final BookingService bookingService;

    @GetMapping
    public String mypage() {
        return "redirect:/mypage/benefits";
    }

    @GetMapping("/benefits")
    public String mypageBenefits(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("tickets", Collections.emptyList());
        model.addAttribute("coupons", Collections.emptyList());
        return "mypage/mypage_benefits";
    }

    @GetMapping("/coupons")
    public String mypageCoupons(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("coupons", Collections.emptyList());
        return "mypage/mypage_coupons";
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
                                @PageableDefault(size = 10) Pageable pageable, // 💡 한 페이지에 10개씩!
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

    @GetMapping("/membership")
    public String mypageMembership(@AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/mypage_membership";
    }

    @GetMapping("/membership/subscribe")
    public String mypageMembershipSubscribe(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestParam(required = false) String plan,
                                            RedirectAttributes rttr) {
        // TODO: membershipService.subscribe(userDetails.getUsername(), plan)
        rttr.addFlashAttribute("successMessage", "멤버십이 신청되었습니다.");
        return "redirect:/mypage/membership";
    }

    @GetMapping("/membership/cancel")
    public String mypageMembershipCancel(@AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes rttr) {
        // TODO: membershipService.cancel(userDetails.getUsername())
        rttr.addFlashAttribute("successMessage", "멤버십이 해지되었습니다.");
        return "redirect:/mypage/membership";
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