package com.ticketflow.controller;

import com.ticketflow.dto.UserUpdateDto;
import com.ticketflow.entity.User;
import com.ticketflow.service.UserService;
import com.ticketflow.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final WishlistService wishlistService;

    @GetMapping
    public String mypage() {
        return "redirect:/mypage/benefits";
    }

    @GetMapping("/benefits")
    public String mypageBenefits(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("wishCount", wishlistService.countByUserId(user.getUserId()));
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
                                Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("tickets", Collections.emptyList());
        return "mypage/mypage_tickets";
    }

    @GetMapping("/tickets/{bookingNo}")
    public String mypageTicketDetail(@PathVariable String bookingNo,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("ticket", Collections.emptyMap());
        return "mypage/mypage_ticket_detail";
    }

    @GetMapping("/membership")
    public String mypageMembership(@AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/mypage_membership";
    }

//    @GetMapping("/membership/subscribe")
//    public String mypageMembershipSubscribe(@AuthenticationPrincipal UserDetails userDetails,
//                                            @RequestParam(required = false) String plan,
//                                            RedirectAttributes rttr) {
//        // TODO: membershipService.subscribe(userDetails.getUsername(), plan)
//        rttr.addFlashAttribute("successMessage", "멤버십이 신청되었습니다.");
//        return "redirect:/mypage/membership";
//    }
    @GetMapping("/membership/subscribe")
    public String mypageMembershipSubscribe(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam(required = false) String plan,
                                        Model model) {

        User user = userService.findByUserId(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("plan", plan);

        return "mypage/mypage_membership_subscribe";
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
        model.addAttribute("wishlist", wishlistService.findWishlistByUserId(user.getUserId()));
        return "mypage/mypage_wishlist";
    }

    @ResponseBody
    @PostMapping("/wishlist/delete")
    public ResponseEntity<?> deleteWishlist(@RequestBody Map<String, List<String>> request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        List<String> concertIds = request.get("concertIds");

        if (concertIds == null || concertIds.isEmpty()) {
            return ResponseEntity.badRequest().body("삭제할 항목이 없습니다.");
        }

        // 로그인한 유저의 ID 확인
        String userId = userDetails.getUsername();

        // 서비스에서 삭제 로직 수행 (본인의 WishlistService 메서드명에 맞게 수정하세요)
        // 예: wishlistService.removeSelectedWishlist(userId, concertIds);
        wishlistService.deleteSelected(userId, concertIds);

        return ResponseEntity.ok().body(Map.of("success", true));
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