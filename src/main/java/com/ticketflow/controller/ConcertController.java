package com.ticketflow.controller;

import com.ticketflow.entity.Concert;
import com.ticketflow.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/concert")
public class ConcertController {

    private final ConcertService concertService;

    // 로그인 체크 공통 메서드
    private boolean checkLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("logged_in"));
    }

    // =========================================================================
    // [1] 화면 이동 (View) 영역
    // =========================================================================

    @GetMapping("/")
    public String mainPage(@RequestParam(required = false) String genre, Model model) {
        List<Concert> concertList;

        if (genre != null && !genre.isEmpty()) {
            String koreanGenre = mapGenreCodeToName(genre);
            concertList = concertService.getConcertsByGenre(koreanGenre);
        } else {
            concertList = concertService.getAllConcerts();
        }

        model.addAttribute("concertList", concertList);
        model.addAttribute("genre", genre);
        return "concert/mainpage";
    }

    private String mapGenreCodeToName(String code) {
        return switch (code) {
            case "jazz" -> "재즈/팝";
            case "ballad" -> "발라드/R&B";
            case "rock" -> "록/메탈";
            case "indie" -> "인디/어쿠스틱";
            case "hiphop" -> "힙합/랩";
            case "etc" -> "기타 대중음악";
            default -> code;
        };
    }

    // ConcertController.java 수정 예시
    @GetMapping("/{id}/detail-page")
    public String concertDetailPage(@PathVariable String id, Model model) {
        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);

        // 공연 기간 로직 (시작일 == 종료일 인 경우 시작일만 표시)
        String dateRange = concert.getConcertStartDate().equals(concert.getConcertEndDate())
                ? concert.getConcertStartDate().toString()
                : concert.getConcertStartDate() + " ~ " + concert.getConcertEndDate();
        model.addAttribute("dateRange", dateRange);

        // [핵심 수정]
        if (concert.getConcertPriceInfo() != null) {
            // 정규식: (숫자+쉼표+숫자) 형식을 제외한 쉼표만 찾아 분리
            // 만약 데이터가 너무 복잡하다면, DB 데이터를 "VIP석 99,000원|GA석 88,000원" 처럼
            // 파이프(|) 구분자로 바꾸는 것이 가장 확실합니다.
            String[] prices = concert.getConcertPriceInfo().split(",(?![0-9])");

            List<String> priceList = Arrays.stream(prices)
                    .map(String::trim)
                    .collect(Collectors.toList());
            model.addAttribute("priceList", priceList);
        }

        return "concert/concert_detail";
    }

    @GetMapping("/seatmap")
    public String seatMapPage() {
        return "concert/seatmap";
    }

    // =========================================================================
    // [2] 순수 데이터 API 영역 (REST API)
    // =========================================================================

    /**
     * [수정] 캘린더 날짜 클릭 시 해당 날짜의 실제 회차(concert_time) 조회
     * DB의 "금요일 14:00" 같은 데이터에서 요일을 제거하고 시간만 추출하여 전달
     */
    @GetMapping("/{id}/sessions")
    @ResponseBody
    public ResponseEntity<?> getSessionsByDate(@PathVariable String id, @RequestParam String date) {
        List<String> rawTimes = concertService.findSessionsByDate(id, date);

        // 데이터가 없으면 빈 리스트 반환
        if (rawTimes == null || rawTimes.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, String>> cleanedSessions = rawTimes.stream().map(time -> {
            // 요일, 공백, 괄호(), 물결표(~), 대시(-) 등을 모두 제거하고 숫자와 콜론만 남김
            String cleanTime = time.replaceAll("[가-힣\\s\\(\\)\\~\\-]", "");
            return Map.of("id", cleanTime, "time", cleanTime);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(cleanedSessions);
    }

    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().body(Map.of("message", "공연 검색 결과 반환"));
    }

    @GetMapping("/suggest")
    @ResponseBody
    public ResponseEntity<?> suggestConcerts(@RequestParam String q) {
        return ResponseEntity.ok().body(Map.of("suggestions", Collections.emptyList()));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getConcertDetail(@PathVariable String id) {
        return ResponseEntity.ok().body(Map.of(
                "id", id,
                "title", "테스트 공연",
                "posterUrl", "/images/poster_dummy.png"
        ));
    }

    @GetMapping("/category/{name}")
    @ResponseBody
    public ResponseEntity<?> getConcertsByCategory(@PathVariable String name) {
        return ResponseEntity.ok().body(Map.of("category", name, "concerts", Collections.emptyList()));
    }

    @GetMapping("/ranking")
    @ResponseBody
    public ResponseEntity<?> getConcertRanking() {
        return ResponseEntity.ok().body(Map.of("ranking", Collections.emptyList()));
    }

    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> addWishlist(@PathVariable String id, @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("message", "위시리스트 추가"));
    }

    @DeleteMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> removeWishlist(@PathVariable String id, @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("message", "위시리스트 취소"));
    }

    @GetMapping("/liked")
    @ResponseBody
    public ResponseEntity<?> getMyWishlist(HttpSession session) {
        if (!checkLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("likedConcerts", Collections.emptyList()));
    }

    @GetMapping("/recommended")
    @ResponseBody
    public ResponseEntity<?> getColdStartRecommendations() {
        return ResponseEntity.ok().body(Map.of("recommended", Collections.emptyList()));
    }

    @GetMapping("/ai-recommend")
    @ResponseBody
    public ResponseEntity<?> getAiRecommendations() {
        return ResponseEntity.ok().body(Map.of("aiRecommended", Collections.emptyList()));
    }
}