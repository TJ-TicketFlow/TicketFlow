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

    @GetMapping("/{id}/detail-page")
    public String concertDetailPage(@PathVariable String id, Model model) {
        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);

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
    public String rankingPage(Model model) {
        List<Map<String, Object>> allRankings = concertService.getRankedConcerts();

        // 헤더에서 active 클래스를 부여하기 위해 genre를 'ranking'으로 설정
        model.addAttribute("genre", "ranking");

        if (allRankings.size() >= 3) {
            model.addAttribute("top3", allRankings.subList(0, 3));
            model.addAttribute("restRankings", allRankings.subList(3, allRankings.size()));
        } else {
            model.addAttribute("top3", allRankings);
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