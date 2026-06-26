package com.ticketflow.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MainController {

    /**
     * 루트("/") 접근 시
     * - 로그인된 경우 → 마이페이지
     * - 비로그인 → 로그인 페이지
     * (SecurityConfig에서 "/" 는 permitAll 이므로 principal이 null일 수 있음)
     */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return "redirect:/mypage/benefits";
        }
        return "redirect:/login";
    }

}