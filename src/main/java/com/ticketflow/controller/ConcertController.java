package com.ticketflow.controller;

import com.ticketflow.entity.Concert;          // 엔티티 임포트
import com.ticketflow.service.ConcertService; // 서비스 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/concert")
public class ConcertController {

    // 서비스 의존성 주입
    private final ConcertService concertService;

    // 로그인 체크 공통 메서드
    private boolean checkLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("logged_in"));
    }

    // =========================================================================
    // [1] 화면 이동 (View) 영역
    // =========================================================================

    /**
     * 메인 페이지 이동
     * GET /concert/
     */
    @GetMapping("/")
    public String mainPage(@RequestParam(required = false) String genre, Model model) {
        List<Concert> concertList;

        if (genre != null && !genre.isEmpty()) {
            // 영문 코드를 한글 DB 값으로 변환
            String koreanGenre = mapGenreCodeToName(genre);
            concertList = concertService.getConcertsByGenre(koreanGenre);
        } else {
            concertList = concertService.getAllConcerts();
        }

        model.addAttribute("concertList", concertList);
        model.addAttribute("genre", genre); // 'ballad' 같은 영문 코드가 저장됨
        return "concert/mainpage";
    }

    // 매핑 로직 추가
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

    /**
     * 🌟 랭킹 페이지 이동
     */
//    @GetMapping("/ranking-page")
//    public String rankingPage(Model model) {
//        // 서비스로부터 예매율 높은 순 Top 20 데이터를 가져옵니다.
//        List<Concert> rankingConcerts = concertService.getRankingTop20();
//        model.addAttribute("rankingList", rankingConcerts);
//        return "concert/ranking";
//    }

    /**
     * 공연 상세 페이지 이동
     */
    @GetMapping("/{id}/detail-page")
    public String concertDetailPage(@PathVariable String id, Model model) {
        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);

        String dateRange = concert.getConcertStartDate().equals(concert.getConcertEndDate())
                ? concert.getConcertStartDate().toString()
                : concert.getConcertStartDate() + " ~ " + concert.getConcertEndDate();
        model.addAttribute("dateRange", dateRange);

        return "concert/concert_detail";
    }

    /**
     * 좌석 선택 페이지 이동
     */
    @GetMapping("/seatmap")
    public String seatMapPage() {
        return "concert/seatmap";
    }


    // =========================================================================
    // [2] 순수 데이터 API 영역 (REST API)
    // =========================================================================

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
                "posterUrl", "/images/poster_dummy.png",
                "sessions", List.of(
                        Map.of("sessionId", "session_001", "date", "2026-06-15", "time", "18:00", "status", "예매가능"),
                        Map.of("sessionId", "session_002", "date", "2026-06-16", "time", "14:00", "status", "매진")
                )
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
    public ResponseEntity<?> addWishlist(@PathVariable String id, HttpSession session) {
        if (!checkLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("message", "위시리스트 추가"));
    }

    @DeleteMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> removeWishlist(@PathVariable String id, HttpSession session) {
        if (!checkLogin(session)) {
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