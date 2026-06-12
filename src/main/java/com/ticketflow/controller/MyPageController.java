package com.ticketflow.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private boolean checkLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("logged_in"));
    }

    @GetMapping
    public String mypage() {
        return "redirect:/mypage/benefits";
    }

    @GetMapping("/benefits")
    public String mypageBenefits(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        model.addAttribute("tickets", DummyData.DUMMY_TICKETS);
        return "mypage/mypage_benefits";
    }

    @GetMapping("/coupons")
    public String mypageCoupons(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        return "mypage/mypage_coupons";
    }

    @GetMapping("/profile")
    public String mypageProfile(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        return "mypage/mypage_profile";
    }

    @PostMapping("/profile")
    public String mypageProfileSave(HttpSession session) {
        if (!checkLogin(session)) return "redirect:/login";
        // 실제 저장 로직은 DB 연동 시 구현
        return "redirect:/mypage/profile";
    }

    @GetMapping("/tickets")
    public String mypageTickets(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        model.addAttribute("tickets", DummyData.DUMMY_TICKETS);
        return "mypage/mypage_tickets";
    }

    @GetMapping("/tickets/{bookingNo}")
    public String mypageTicketDetail(@PathVariable String bookingNo, HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";

        Map<String, Object> ticketDetail = DummyData.DUMMY_TICKETS.stream()
                .filter(t -> t.get("booking_no").equals(bookingNo))
                .findFirst()
                .orElse(DummyData.DUMMY_TICKETS.get(0));

        model.addAttribute("user", DummyData.DUMMY_USER);
        model.addAttribute("ticket", ticketDetail);
        return "mypage/mypage_ticket_detail";
    }

    @GetMapping("/membership")
    public String mypageMembership(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        return "mypage/mypage_membership";
    }

    @GetMapping("/membership/subscribe")
    public String mypageMembershipSubscribe(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        return "mypage/mypage_membership";
    }

    @GetMapping("/membership/cancel")
    public String mypageMembershipCancel(HttpSession session) {
        if (!checkLogin(session)) return "redirect:/login";
        return "redirect:/mypage/membership";
    }

    @GetMapping("/wishlist")
    public String mypageWishlist(HttpSession session, Model model) {
        if (!checkLogin(session)) return "redirect:/login";
        model.addAttribute("user", DummyData.DUMMY_USER);
        return "mypage/mypage_wishlist";
    }

    @GetMapping("/withdraw")
    public String withdraw(HttpSession session) {
        if (!checkLogin(session)) return "redirect:/login";
        session.invalidate();
        return "redirect:/login";
    }
}
