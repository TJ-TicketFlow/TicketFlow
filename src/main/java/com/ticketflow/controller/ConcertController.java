package com.ticketflow.controller;

import com.ticketflow.dto.ConcertResponseDto;
import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.service.ConcertService;
import com.ticketflow.service.MembershipService;
import com.ticketflow.service.StatsService;
import com.ticketflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

// ... 상단 생략 ...

@Controller
@RequiredArgsConstructor
@RequestMapping("/concert")
public class ConcertController {

    private final ConcertService concertService;
    private final MembershipService membershipService;
    private final UserService userService;
    private final com.ticketflow.service.CancelPredictionService cancelPredictionService;
    private final com.ticketflow.repository.PayRepository payRepository;
    private final StatsService statsService;

    // 💡 1. 방금 만든 ONNX 예측 서비스 추가 주입!
    private final com.ticketflow.service.ConcertPredictService concertPredictService;

    @GetMapping("/")
    public String mainPage(@RequestParam(required = false) String genre, Model model, Principal principal) {
        model.addAttribute("isLoggedIn", (principal != null));
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);

        List<Concert> upcomingEntities;
        List<Concert> pastEntities;

        if (genre != null && !genre.isEmpty()) {
            String koreanGenre = mapGenreCodeToName(genre);
            List<Concert> genreConcerts = concertService.getConcertsByGenre(koreanGenre);

            upcomingEntities = genreConcerts.stream()
                    .filter(c -> c.getConcertEndDate().isAfter(today) || c.getConcertEndDate().isEqual(today))
                    .collect(Collectors.toList());
            pastEntities = genreConcerts.stream()
                    .filter(c -> c.getConcertEndDate().isBefore(today))
                    .collect(Collectors.toList());
        } else {
            upcomingEntities = concertService.getUpcomingConcerts();
            pastEntities = concertService.getPastConcerts();
        }

        List<ConcertResponseDto> upcomingConcerts = upcomingEntities.stream()
                .map(ConcertResponseDto::new)
                .toList();
        List<ConcertResponseDto> pastConcerts = pastEntities.stream()
                .map(ConcertResponseDto::new)
                .toList();

        // 💡 2. [수정 구역] 가짜 mockRate 대신 실제 AI 모델의 예측값을 매칭시킵니다.
        for (int i = 0; i < upcomingEntities.size(); i++) {
            Concert entity = upcomingEntities.get(i);
            ConcertResponseDto dto = upcomingConcerts.get(i);

            // 실제 데이터베이스 내부의 공연 데이터를 기반으로 AI 스코어 계산
            double realAiRate = concertPredictService.predictSoldOutRate(entity.getConcertId());
            dto.setPredictSoldOutRate(realAiRate);
        }

        for (ConcertResponseDto dto : pastConcerts) {
            dto.setPredictSoldOutRate(0.0);
        }

        model.addAttribute("upcomingConcerts", upcomingConcerts);
        model.addAttribute("pastConcerts", pastConcerts);
        model.addAttribute("genre", genre);

        return "concert/mainpage";
    }

    // ... 하단 생략 ...


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
        try {
            statsService.updateStats(id);
        } catch (Exception e) {
            System.err.println("상세 페이지 진입 시 통계 갱신 오류: " + e.getMessage());
        }

        Concert concert = concertService.findById(id);
        model.addAttribute("concert", concert);
        model.addAttribute("stats", concertService.getStatsData(id));
        model.addAttribute("today", LocalDate.now());

        boolean isAllSoldOut = concertService.isAllSoldOut(id);
        model.addAttribute("isAllSoldOut", isAllSoldOut);

        boolean isLiked = false;
        if (principal != null) {
            String userId = principal.getName();
            isLiked = concertService.isLiked(id, userId);
        }
        model.addAttribute("isLiked", isLiked);

        int wishCount = concertService.getWishlistCount(id);
        model.addAttribute("wishCount", wishCount);

        String dateRange = concert.getConcertStartDate().equals(concert.getConcertEndDate())
                ? concert.getConcertStartDate().toString()
                : concert.getConcertStartDate() + " ~ " + concert.getConcertEndDate();
        model.addAttribute("dateRange", dateRange);

        if (concert.getConcertPriceInfo() != null) {
            String[] prices = concert.getConcertPriceInfo().split(",(?![0-9])");
            model.addAttribute("priceList", Arrays.stream(prices).map(String::trim).collect(Collectors.toList()));
        }

        if (principal != null) {
            model.addAttribute("isLoggedIn", true);
            User user = userService.findByUserId(principal.getName());
            double baseDiscount = membershipService.getDiscountRate(user);

            List<UserCoupon> availableCoupons = user.getUserCoupons().stream()
                    .filter(uc -> uc.getUserCouponStatus() == 0)
                    .collect(Collectors.toList());

            model.addAttribute("baseDiscount", (int)(baseDiscount * 100));
            model.addAttribute("couponCount", availableCoupons.size());
            model.addAttribute("hasBenefit", baseDiscount > 0 || !availableCoupons.isEmpty());
            model.addAttribute("coupons", availableCoupons);
        } else {
            model.addAttribute("isLoggedIn", false);
            model.addAttribute("hasBenefit", false);
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
        LocalDate localDate = LocalDate.parse(date);
        List<String> rawTimes = concertService.findSessionsByDate(id, date);
        if (rawTimes == null || rawTimes.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<Map<String, Object>> sessionData = rawTimes.stream().map(time -> {
            String cleanTime = time.replaceAll("[가-힣\\s\\(\\)\\~\\-]", "");
            boolean isSoldOut = concertService.isSessionSoldOut(id, cleanTime, localDate);

            Map<String, Object> map = new HashMap<>();
            map.put("id", cleanTime);
            map.put("time", cleanTime);
            map.put("soldOut", isSoldOut);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(sessionData);
    }

    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> toggleWishlist(@PathVariable String id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        String userId = principal.getName();
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
    public ResponseEntity<?> getMyWishlist(Principal principal) { // 💡 HttpSession 대신 Principal을 받습니다.
        // 💡 주입받은 principal이 null이면 로그인되지 않은 상태입니다.
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }

        // 로그인된 경우의 로직 (현재는 빈 리스트 반환)
        return ResponseEntity.ok().body(Map.of("likedConcerts", Collections.emptyList()));
    }

    @GetMapping("/recommended")
    @ResponseBody
    public ResponseEntity<?> getGeneralRecommendations() {
        List<ConcertResponseDto> popular = concertService.getPopularConcerts(3);
        return ResponseEntity.ok(Map.of("recommended", popular));
    }

    @GetMapping("/ai-recommend")
    @ResponseBody
    public ResponseEntity<?> getPersonalizedRecommendations(Principal principal) {
        if (principal == null) {
            return getGeneralRecommendations();
        }
        List<ConcertResponseDto> personalized = concertService.getRecommendedConcerts(principal.getName());
        return ResponseEntity.ok(Map.of("aiRecommended", personalized));
    }

    @GetMapping("/{id}/stats-json")
    @ResponseBody
    public ResponseEntity<?> getStatsJson(@PathVariable String id) {
        Object stats = concertService.getStatsData(id);
        if (stats == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/available-dates")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableDates(@PathVariable String id) {
        List<String> dates = concertService.findAvailableDates(id);
        return ResponseEntity.ok(dates != null ? dates : Collections.emptyList());
    }

    @Cacheable(value = "cancelRateCache", key = "#id")
    @GetMapping("/{id}/cancel-rate")
    @ResponseBody
    public ResponseEntity<Double> getConcertCancelRate(@PathVariable String id) {
        try {
            List<Pay> concertPays = payRepository.findValidPaysByConcertId(id);
            double cancelRate = cancelPredictionService.calculatePerformanceCancelRate(concertPays);
            double roundedRate = Math.round(cancelRate * 100.0) / 100.0;

            return ResponseEntity.ok(roundedRate);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}