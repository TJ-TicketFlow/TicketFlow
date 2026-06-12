package com.ticketflow.controller;
import com.ticketflow.entity.Concert;
import com.ticketflow.service.ConcertService;
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

    private final ConcertService concertService;

    // 로그인 체크 공통 메서드 (MyPageController 구조 연동)
    private boolean checkLogin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("logged_in"));
    }

    // =========================================================================
    // [1] 화면 이동 (View) 영역 - templates/concert/ 안의 HTML 파일들과 매핑
    // =========================================================================

    /**
     * 메인 페이지 이동
     * GET /concert/main
     */
    @GetMapping("/main")
    public String mainPage() {
        // src/main/resources/templates/concert/mainpage.html 화면을 열어줍니다.
        return "concert/mainpage";
    }

    /**
     * 🌟 [추가] 랭킹 페이지 이동
     * GET /concert/ranking-page
     * 메인페이지의 '랭킹 더보기' 버튼을 누르면 이 주소로 이동합니다.
     */
//    @GetMapping("/ranking-page")
//    public String rankingPage(Model model) {
//        // 서비스로부터 예매율 높은 순 Top 20 데이터를 가져옵니다.
//        List<Concert> rankingConcerts = concertService.getRankingTop20();
//        // 현재 concertService에 getRankingTop20 이라는 것이 없어서 생긴 오류
//
//        // 타임리프 템플릿 엔진에서 사용할 수 있도록 Model에 담아줍니다.
//        model.addAttribute("rankingList", rankingConcerts);
//
//        // src/main/resources/templates/concert/ranking.html 화면을 열어줍니다.
//        return "concert/ranking";
//    }
    /**
     * 공연 상세 페이지 이동
     * GET /concert/{id}/detail-page
     * 메인페이지에서 포스터나 공연 이름을 클릭하면 이 주소로 이동합니다.
     */
    @GetMapping("/{id}/detail-page")
    public String concertDetailPage(@PathVariable String id, Model model) {
        // 타임리프 Model에 공연 ID를 심어서 보냅니다.
        // concert_detail.html 안의 자바스크립트가 이 ID를 추출하여 아래 3번 API를 비동기 호출하게 됩니다.
        model.addAttribute("concertId", id);

        // src/main/resources/templates/concert/concert_detail.html 화면을 열어줍니다.
        return "concert/concert_detail";
    }

    /**
     * 좌석 선택 페이지 이동
     * GET /concert/seatmap
     * 상세페이지에서 날짜/시간 선택 후 [예매하기] 결과가 성공하면 프론트엔드에서 이 화면으로 이동시킵니다.
     */
    @GetMapping("/seatmap")
    public String seatMapPage() {
        // src/main/resources/templates/concert/seatmap.html 화면을 열어줍니다.
        return "concert/seatmap";
    }


    // =========================================================================
    // [2] 순수 데이터 API 영역 (REST API) - JSON 반환 (Elasticsearch 연동 예정)
    // =========================================================================

    /**
     * 1. 공연 목록 검색 (메인페이지 검색창 엔터 혹은 검색 버튼 클릭 시)
     * GET /concert/search
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        // TODO: Elasticsearch 검색 쿼리 수행 로직이 들어갈 자리입니다.
        return ResponseEntity.ok().body(Map.of("message", "공연 검색 결과 반환"));
    }

    /**
     * 2. 검색어 자동완성 (메인페이지 검색창 입력 시 실시간 반영)
     * GET /concert/suggest
     */
    @GetMapping("/suggest")
    @ResponseBody
    public ResponseEntity<?> suggestConcerts(@RequestParam String q) {
        // TODO: ES completion suggester 연동 로직이 들어갈 자리입니다.
        return ResponseEntity.ok().body(Map.of("suggestions", Collections.emptyList()));
    }

    /**
     * 3. 공연 상세 정보 조회 (상세페이지 로딩 시 날짜/시간 버튼 생성을 위한 정보 제공)
     * GET /concert/{id}
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getConcertDetail(@PathVariable String id) {
        // TODO: ES 단건 조회 및 날짜별 회차(sessions) 가공 로직이 들어갈 자리입니다.
        // 예매 팀원의 '임시 예매 생성(/booking/create)'과 연동하기 위해 고유한 sessionId를 함께 내려주어야 합니다.
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

    /**
     * 4. 카테고리별 공연 목록 조회
     * GET /concert/category/{name}
     */
    @GetMapping("/category/{name}")
    @ResponseBody
    public ResponseEntity<?> getConcertsByCategory(@PathVariable String name) {
        return ResponseEntity.ok().body(Map.of("category", name, "concerts", Collections.emptyList()));
    }

    /**
     * 5. 인기 공연 랭킹 조회
     * GET /concert/ranking
     */
    @GetMapping("/ranking")
    @ResponseBody
    public ResponseEntity<?> getConcertRanking() {
        return ResponseEntity.ok().body(Map.of("ranking", Collections.emptyList()));
    }

    /**
     * 6. 위시리스트(좋아요) 추가
     * POST /concert/{id}/like
     */
    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> addWishlist(@PathVariable String id, HttpSession session) {
        if (!checkLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("message", "위시리스트 추가"));
    }

    /**
     * 7. 위시리스트(좋아요) 취소
     * DELETE /concert/{id}/like
     */
    @DeleteMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<?> removeWishlist(@PathVariable String id, HttpSession session) {
        if (!checkLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("message", "위시리스트 취소"));
    }

    /**
     * 8. 내 위시리스트 목록 조회 (MyPageController 화면 연동용)
     * GET /concert/liked
     */
    @GetMapping("/liked")
    @ResponseBody
    public ResponseEntity<?> getMyWishlist(HttpSession session) {
        if (!checkLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok().body(Map.of("likedConcerts", Collections.emptyList()));
    }

    /**
     * 9. 좋아요 많은순 추천 (콜드스타트용)
     * GET /concert/recommended
     */
    @GetMapping("/recommended")
    @ResponseBody
    public ResponseEntity<?> getColdStartRecommendations() {
        return ResponseEntity.ok().body(Map.of("recommended", Collections.emptyList()));
    }

    /**
     * 10. 공연 추천 (AI)
     * GET /concert/ai-recommend
     */
    @GetMapping("/ai-recommend")
    @ResponseBody
    public ResponseEntity<?> getAiRecommendations() {
        return ResponseEntity.ok().body(Map.of("aiRecommended", Collections.emptyList()));
    }
}