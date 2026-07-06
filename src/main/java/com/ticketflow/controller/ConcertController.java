package com.ticketflow.controller;

import com.ticketflow.dto.ConcertResponseDto;
import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.ConcertRepository;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.WishlistRepository;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/concert")
public class ConcertController {

    private final ConcertService concertService;
    private final MembershipService membershipService;
    private final UserService userService;
    private final ConcertRepository concertRepository;
    private final com.ticketflow.service.CancelPredictionService cancelPredictionService;
    private final com.ticketflow.repository.PayRepository payRepository;
    private final StatsService statsService;
    private final WishlistRepository wishlistRepository;

    // 💡 1. 방금 만든 ONNX 예측 서비스 추가 주입!
    private final com.ticketflow.service.ConcertPredictService concertPredictService;

    @GetMapping("/")
    public String mainPage(@RequestParam(required = false) String genre, Model model, Principal principal) {
        // [중요] 로그인 여부를 모델에 전달 (이게 없으면 JS에서 isLoggedIn이 null/false로 인식됨)
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
        // 🌟 [핵심] 상세 페이지 진입 시점에 최신 통계 강제 갱신
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
            model.addAttribute("couponCount", availableCoupons.size()); // 필터링된 개수 사용
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
        // 1. 데이터 가져오기
        List<Map<String, Object>> rankings = (genre != null && !genre.isEmpty())
                ? concertService.getRankedConcertsByGenre(mapGenreCodeToName(genre))
                : concertService.getRankedConcerts();

        // 2. null 체크
        if (rankings == null) rankings = Collections.emptyList();

        // 로그 추가 (실제 들어오는지 확인)
        System.out.println("★ 랭킹 데이터 개수: " + rankings.size());

        // 3. 랭킹 분리 (가장 안전한 방식)
        int size = rankings.size();
        List<Map<String, Object>> top3 = size >= 3 ? rankings.subList(0, 3) : rankings;
        List<Map<String, Object>> restRankings = size > 3 ? rankings.subList(3, size) : Collections.emptyList();

        model.addAttribute("top3", top3);
        model.addAttribute("restRankings", restRankings);
        model.addAttribute("genre", genre == null ? "all" : genre);

        return "concert/ranking";
    }

    // =========================================================================
    // [2] 순수 데이터 API 영역 (REST API)
    // =========================================================================

    // 먼저 DTO가 없다면 임시로 Map을 사용하거나, DTO 클래스를 생성하세요.
// 아래는 DTO 없이 Map으로 처리하는 예시입니다.

    @GetMapping("/{id}/sessions")
    @ResponseBody
    public ResponseEntity<?> getSessionsByDate(@PathVariable String id, @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<String> rawTimes = concertService.findSessionsByDate(id, date);
        if (rawTimes == null || rawTimes.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        // [수정된 부분] 매진 여부를 확인하여 맵에 담아 반환
        List<Map<String, Object>> sessionData = rawTimes.stream().map(time -> {
            // 시간에서 불필요한 문자 제거 (기존 로직 유지)
            String cleanTime = time.replaceAll("[가-힣\\s\\(\\)\\~\\-]", "");

            // 날짜를 포함해서 매진 확인
            boolean isSoldOut = concertService.isSessionSoldOut(id, cleanTime, localDate);

            Map<String, Object> map = new HashMap<>();
            map.put("id", cleanTime);
            map.put("time", cleanTime);
            map.put("soldOut", isSoldOut); // 이 정보가 프론트로 전달됩니다.
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(sessionData);
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

        // 이 로그를 꼭 확인하세요!
        System.out.println("★ 검색된 공연 리스트 사이즈: " + (concertList != null ? concertList.size() : "null"));

        model.addAttribute("today", LocalDate.now());
        model.addAttribute("concertList", concertList);
        model.addAttribute("keyword", keyword);

        return "concert/search_results";
    }

    @GetMapping("/suggest")
    @ResponseBody
    public ResponseEntity<?> suggestConcerts(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("suggestions", Collections.emptyList()));
        }
        // 서비스에서 만든 엘라스틱서치 자동완성 기능 호출
        List<String> suggestions = concertService.autocomplete(q);
        return ResponseEntity.ok(Map.of("suggestions", suggestions));
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

    // 1. 기존 메서드: 로그인 여부와 관계없이 '일반적인 추천(인기순)' 반환
    @GetMapping("/recommended")
    @ResponseBody
    public ResponseEntity<?> getRecommended(Principal principal) {
        List<ConcertResponseDto> list = new ArrayList<>();

        // 1. 개인화 추천 시도
        if (principal != null) {
            long wishlistCount = wishlistRepository.countByUser_UserId(principal.getName());
            if (wishlistCount > 0) {
                list = concertService.getRecommendedConcerts(principal.getName());
            }
        }

        // 2. 데이터가 없을 경우 전체 데이터에서 최신순으로 가져오기
        if (list == null || list.isEmpty()) {
            list = concertRepository.findAll().stream()
                    // 💡 여기에 실제 엔티티의 필드명인 concertStartDate 사용
                    .sorted(Comparator.comparing(Concert::getConcertStartDate).reversed())
                    .limit(3)
                    .map(ConcertResponseDto::new)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(Map.of("recommended", list));
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
        // 예: concertService에 findAvailableDatesByConcertId(id) 메서드 추가 필요
        List<String> dates = concertService.findAvailableDates(id);
        return ResponseEntity.ok(dates != null ? dates : Collections.emptyList());
    }

    @GetMapping("/sync-elasticsearch")
    @ResponseBody
    public String syncElastic() {
        List<Concert> allConcerts = concertRepository.findAll();
        for (Concert concert : allConcerts) {
            concertService.saveConcert(concert);
        }
        return "성공! 총 " + allConcerts.size() + "개의 데이터를 엘라스틱서치에 넣었습니다.";
    }

    @Cacheable(value = "cancelRateCache", key = "#id")
    @GetMapping("/{id}/cancel-rate")
    @ResponseBody
    public ResponseEntity<Double> getConcertCancelRate(@PathVariable String id) {
        try {
            // 1. 해당 콘서트의 결제 완료 내역 가져오기
            List<Pay> concertPays = payRepository.findValidPaysByConcertId(id);

            // 2. 머신러닝 예측 돌리기
            double cancelRate = cancelPredictionService.calculatePerformanceCancelRate(concertPays);

            // 3. 소수점 둘째 자리까지만 예쁘게 자르기
            double roundedRate = Math.round(cancelRate * 100.0) / 100.0;

            return ResponseEntity.ok(roundedRate);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}