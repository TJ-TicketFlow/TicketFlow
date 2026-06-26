package com.ticketflow.controller;

import com.ticketflow.dto.ConcertResponseDto;
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

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/concert")
public class ConcertController {

    private final ConcertService concertService;

    private boolean checkLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("logged_in"));
    }

    // =========================================================================
    // [1] 화면 이동 (View) 영역
    // =========================================================================

    @GetMapping("/")
    public String mainPage(@RequestParam(required = false) String genre, Model model, Principal principal) {
        // [중요] 로그인 여부를 모델에 전달 (이게 없으면 JS에서 isLoggedIn이 null/false로 인식됨)
        model.addAttribute("isLoggedIn", (principal != null));
        LocalDate today = LocalDate.now(); // 추가
        model.addAttribute("today", today); // 추가

        if (genre != null && !genre.isEmpty()) {
            String koreanGenre = mapGenreCodeToName(genre);
            List<Concert> genreConcerts = concertService.getConcertsByGenre(koreanGenre);

            List<Concert> upcoming = genreConcerts.stream()
                    .filter(c -> c.getConcertEndDate().isAfter(today) || c.getConcertEndDate().isEqual(today))
                    .collect(Collectors.toList());
            List<Concert> past = genreConcerts.stream()
                    .filter(c -> c.getConcertEndDate().isBefore(today))
                    .collect(Collectors.toList());

            model.addAttribute("upcomingConcerts", upcoming);
            model.addAttribute("pastConcerts", past);
        } else {
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
    public String concertDetailPage(@PathVariable String id, Model model, Principal principal) {
        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);
        model.addAttribute("stats", concertService.getStatsData(id));
        model.addAttribute("today", LocalDate.now());

        // [추가] 전체 매진 여부 확인 (서비스에 해당 로직 구현 필요)
        boolean isAllSoldOut = concertService.isAllSoldOut(id);
        model.addAttribute("isAllSoldOut", isAllSoldOut);

        // 1. 좋아요 여부 확인
        boolean isLiked = false;
        if (principal != null) {
            String userId = principal.getName();
            isLiked = concertService.isLiked(id, userId);
        }
        model.addAttribute("isLiked", isLiked);

        // 2. [추가] 실시간 위시리스트 개수 가져오기 (DB에서 직접 조회)
        int wishCount = concertService.getWishlistCount(id);
        model.addAttribute("wishCount", wishCount);

        // 날짜 및 가격 정보 처리 (기존과 동일)
        String dateRange = concert.getConcertStartDate().equals(concert.getConcertEndDate())
                ? concert.getConcertStartDate().toString()
                : concert.getConcertStartDate() + " ~ " + concert.getConcertEndDate();
        model.addAttribute("dateRange", dateRange);

        if (concert.getConcertPriceInfo() != null) {
            String[] prices = concert.getConcertPriceInfo().split(",(?![0-9])");
            model.addAttribute("priceList", Arrays.stream(prices).map(String::trim).collect(Collectors.toList()));
        }

        if (principal != null) {
            // 로그인 상태: 멤버십 혜택 표시
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("isMember", true); // 멤버십 가입자라고 가정

            // 쿠폰 0장으로 초기화 (나중에 서비스 로직으로 대체 가능)
            Map<Integer, Integer> coupons = new TreeMap<>();
            coupons.put(5, 0);
            coupons.put(10, 0);
            coupons.put(15, 0);
            model.addAttribute("coupons", coupons);
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        return "concert/concert_detail";
    }


    @GetMapping("/ranking")
    public String rankingPage(@RequestParam(required = false) String genre, Model model) {
        List<Map<String, Object>> rankings = (genre != null && !genre.isEmpty())
                ? concertService.getRankedConcertsByGenre(mapGenreCodeToName(genre))
                : concertService.getRankedConcerts();

        model.addAttribute("genre", genre == null ? "all" : genre);
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
        if (rawTimes == null || rawTimes.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<Map<String, String>> cleanedSessions = rawTimes.stream().map(time -> {
            String cleanTime = time.replaceAll("[가-힣\\s\\(\\)\\~\\-]", "");
            return Map.of("id", cleanTime, "time", cleanTime);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(cleanedSessions);
    }

    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> toggleWishlist(@PathVariable String id, Principal principal) { // HttpSession 대신 Principal 사용

        // 1. Principal이 null이면 로그인 안 된 상태
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }

        // 2. 로그인된 유저의 아이디 추출
        String userId = principal.getName();

        // 3. 서비스 로직 수행
        boolean isLiked = concertService.toggleWishlist(id, userId);
        int newCount = concertService.getWishlistCount(id);

        return ResponseEntity.ok().body(Map.of("isLiked", isLiked, "newCount", newCount));
    }

    @GetMapping("/search")
    public String searchConcerts(@RequestParam(required = false) String keyword, Model model) {
        if (keyword == null || keyword.isEmpty()) {
            return "redirect:/concert/";
        }

        List<Concert> concertList = concertService.search(keyword);

        // [수정] 현재 날짜를 모델에 추가
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("concertList", concertList);
        model.addAttribute("keyword", keyword);

        return "concert/search_results";
    }

    @GetMapping("/suggest")
    @ResponseBody
    public ResponseEntity<?> suggestConcerts(@RequestParam String q) {
        return ResponseEntity.ok().body(Map.of("suggestions", Collections.emptyList()));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getConcertDetail(@PathVariable String id) {
        return ResponseEntity.ok().body(Map.of("id", id, "title", "테스트 공연", "posterUrl", "/images/poster_dummy.png"));
    }

    @GetMapping("/category/{name}")
    @ResponseBody
    public ResponseEntity<?> getConcertsByCategory(@PathVariable String name) {
        return ResponseEntity.ok().body(Map.of("category", name, "concerts", Collections.emptyList()));
    }

    @GetMapping("/liked")
    @ResponseBody
    public ResponseEntity<?> getMyWishlist(HttpSession session) {
        if (!checkLogin(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        return ResponseEntity.ok().body(Map.of("likedConcerts", Collections.emptyList()));
    }

    // 1. 기존 메서드: 로그인 여부와 관계없이 '일반적인 추천(인기순)' 반환
    @GetMapping("/recommended")
    @ResponseBody
    public ResponseEntity<?> getGeneralRecommendations() {
        // 예: 전체 공연 중 가장 찜이 많은 공연 TOP 3
        List<ConcertResponseDto> popular = concertService.getPopularConcerts(3);
        return ResponseEntity.ok(Map.of("recommended", popular));
    }

    // 2. 새 메서드: 로그인한 유저만 위한 '개인화 추천'
    @GetMapping("/ai-recommend")
    @ResponseBody
    public ResponseEntity<?> getPersonalizedRecommendations(Principal principal) {
        if (principal == null) {
            // 로그인 안 했으면 그냥 일반 추천을 호출하거나 빈 리스트 반환
            return getGeneralRecommendations();
        }
        List<ConcertResponseDto> personalized = concertService.getRecommendedConcerts(principal.getName());
        return ResponseEntity.ok(Map.of("aiRecommended", personalized));
    }

    @GetMapping("/{id}/stats-json")
    @ResponseBody
    public ResponseEntity<?> getStatsJson(@PathVariable String id) {
        // 기존에 model에 담아주던 데이터를 그대로 가져옵니다.
        Object stats = concertService.getStatsData(id);
        if (stats == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/available-dates")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableDates(@PathVariable String id) {
        // 예: concertService에 findAvailableDatesByConcertId(id) 메서드 추가 필요
        List<String> dates = concertService.findAvailableDates(id);
        return ResponseEntity.ok(dates != null ? dates : Collections.emptyList());
    }

}