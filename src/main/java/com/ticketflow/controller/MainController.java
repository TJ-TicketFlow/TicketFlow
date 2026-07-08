package com.ticketflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;

@Controller
public class MainController {

    //롬복(Lombok) 플러그인 에러를 방지하기 위해 final 변수를 선언하고
    private final com.ticketflow.service.ConcertService concertService;

    //자바 순정 생성자를 직접 만들어서 스프링이 서비스를 강제로 주입하도록 합니다. (빨간줄 무조건 소멸)
    public MainController(com.ticketflow.service.ConcertService concertService) {
        this.concertService = concertService;
    }

//    /**
//     * 루트("/") 접근 시
//     */
//    @GetMapping("/")
//    public String index(@AuthenticationPrincipal UserDetails userDetails) {
//        if (userDetails != null) {
//            return "redirect:/mypage/benefits";
//        }
//        return "redirect:/login";
//    }

    @GetMapping("/main")
    public String mainPage(Model model) {

        // 1. 서비스에서 공연 엔티티(Concert) 리스트를 가져옵니다.
        List<com.ticketflow.entity.Concert> upcomingEntities = concertService.getUpcomingConcerts();

        // 2. 엔티티 리스트를 DTO 리스트로 변환합니다. (이제 posterUrl 필드가 활성화됩니다)
        List<com.ticketflow.dto.ConcertResponseDto> upcomingConcerts = upcomingEntities.stream()
                .map(com.ticketflow.dto.ConcertResponseDto::new)
                .toList();

        // 3. [테스트용] 가짜 예매율 데이터 주입
        double mockRate = 85.4;
        for (com.ticketflow.dto.ConcertResponseDto dto : upcomingConcerts) {
            dto.setPredictSoldOutRate(mockRate);
            mockRate += 3.2;
        }

        // 4. 지난 공연 리스트 안전하게 빈 값 처리
        List<com.ticketflow.dto.ConcertResponseDto> pastConcerts = java.util.Collections.emptyList();

        // 5. 데이터를 모델에 담아 화면으로 전달
        model.addAttribute("upcomingConcerts", upcomingConcerts);
        model.addAttribute("pastConcerts", pastConcerts);

        // 주의: templates 폴더 안의 파일명이 mainPage.html(P가 대문자)이면 "mainPage"로 고쳐주세요!
        return "mainpage";
    }
}