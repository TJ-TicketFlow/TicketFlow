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

import java.time.LocalDate;
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
        // 1. 장르별 필터링이 필요한 경우와 전체 메인 페이지 로직 분리
        if (genre != null && !genre.isEmpty()) {
            String koreanGenre = mapGenreCodeToName(genre);
            List<Concert> genreConcerts = concertService.getConcertsByGenre(koreanGenre);
            model.addAttribute("concertList", genreConcerts);
        } else {
            // 필터가 없을 때는 우리가 새로 만든 서비스 메서드 활용
            model.addAttribute("upcomingConcerts", concertService.getUpcomingConcerts());
            model.addAttribute("pastConcerts", concertService.getPastConcerts());
        }

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

    @GetMapping("/{id}/detail-page")
    public String concertDetailPage(@PathVariable String id, Model model) {
        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);

        // [추가] 통계 데이터 바인딩
        model.addAttribute("stats", concertService.getStatsData(id));

        model.addAttribute("today", LocalDate.now());

        String dateRange = concert.getConcertStartDate().equals(concert.getConcertEndDate())
                ? concert.getConcertStartDate().toString()
                : concert.getConcertStartDate() + " ~ " + concert.getConcertEndDate();
        model.addAttribute("dateRange", dateRange);

        if (concert.getConcertPriceInfo() != null) {
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

    @GetMapping("/ranking")
    public String rankingPage(@RequestParam(required = false) String genre, Model model) {
        // 장르가 있으면 변환 후 필터링, 없으면 전체 랭킹 조회
        List<Map<String, Object>> rankings;
        if (genre != null && !genre.isEmpty()) {
            String koreanGenre = mapGenreCodeToName(genre);
            rankings = concertService.getRankedConcertsByGenre(koreanGenre);
        } else {
            rankings = concertService.getRankedConcerts();
        }

        model.addAttribute("genre", genre == null ? "all" : genre); // 탭 활성화를 위해 전달

        if (rankings.size() >= 3) {
            model.addAttribute("top3", rankings.subList(0, 3));
            model.addAttribute("restRankings", rankings.subList(3, rankings.size()));
        } else {
            model.addAttribute("top3", rankings);
            model.addAttribute("restRankings", Collections.emptyList());
        }
        return "concert/ranking";
    }

    // =========================================================================
    // [2] 순수 데이터 API 영역 (REST API)
    // =========================================================================

    @GetMapping("/{id}/sessions")
    @ResponseBody
    public ResponseEntity<?> getSessionsByDate(@PathVariable String id, @RequestParam String date) {
        List<String> rawTimes = concertService.findSessionsByDate(id, date);

        if (rawTimes == null || rawTimes.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, String>> cleanedSessions = rawTimes.stream().map(time -> {
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